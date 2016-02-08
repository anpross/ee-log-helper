package de.anpross.eeloghelper;

import java.util.List;
import java.util.logging.Level;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.LineComment;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import de.anpross.eeloghelper.dtos.LineCommentDto;
import de.anpross.eeloghelper.dtos.MethodDto;
import de.anpross.eeloghelper.enums.MethodAnnotationEnum;
import de.anpross.eeloghelper.enums.MethodStateEnum;
import de.anpross.eeloghelper.visitors.ClassVisitor;
import de.anpross.eeloghelper.visitors.LineCommentVisitor;

@SuppressWarnings("unchecked")
public class EeLogHandler extends AbstractHandler {

	private static final String JDT_NATURE = "org.eclipse.jdt.core.javanature";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		// Get all projects in the workspace
		IProject[] projects = root.getProjects();
		// Loop over all projects
		for (IProject project : projects) {
			try {
				if (project.isNatureEnabled(JDT_NATURE)) {
					analyseMethods(project);
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return null;
	}

	private void analyseMethods(IProject project)
			throws JavaModelException, MalformedTreeException, BadLocationException {
		IPackageFragment[] packages = JavaCore.create(project).getPackageFragments();

		for (IPackageFragment mypackage : packages) {
			if (mypackage.getKind() == IPackageFragmentRoot.K_SOURCE) {
				createAST(mypackage);
			}
		}
	}

	private void createAST(IPackageFragment mypackage)
			throws JavaModelException, MalformedTreeException, BadLocationException {
		for (ICompilationUnit unit : mypackage.getCompilationUnits()) {
			String[] compilationUnitSource = unit.getSource().split("\n");
			CompilationUnit parsedCompilationUnit = parse(unit);
			List<AbstractTypeDeclaration> classes = parsedCompilationUnit.types();
			ASTRewrite rewrite = ASTRewrite.create(parsedCompilationUnit.getAST());
			addLoggerImportToCompUnit(parsedCompilationUnit, parsedCompilationUnit.getAST(), rewrite);
			for (AbstractTypeDeclaration currClass : classes) {
				processClass(rewrite, currClass, parsedCompilationUnit, compilationUnitSource);
			}
			TextEdit edits = rewrite.rewriteAST();
			Document document = new Document(unit.getSource());
			edits.apply(document);

			unit.getBuffer().setContents(document.get());
		}
	}

	private void processClass(ASTRewrite rewrite, AbstractTypeDeclaration currClass,
			CompilationUnit parsedCompilationUnit, String[] compilationUnitSource) {
		ClassVisitor classVisitor = new ClassVisitor(parsedCompilationUnit);
		currClass.accept(classVisitor);

		List<?> commentList = parsedCompilationUnit.getCommentList();
		// Comments need special care so they get sorta-picked up by the Vistor
		LineCommentVisitor lineCommentVisitor = new LineCommentVisitor(parsedCompilationUnit, compilationUnitSource);
		for (Object object : commentList) {
			if (object instanceof LineComment) {
				LineComment currLine = (LineComment) object;
				currLine.accept(lineCommentVisitor);
			}
		}

		List<MethodDto> methods = classVisitor.getMethods();
		List<FieldDeclaration> classVariables = classVisitor.getClassVariables();
		List<LineCommentDto> comments = lineCommentVisitor.getMethodComments();
		corelateMethodsWithComments(methods, comments);

		if (!methods.isEmpty()) {
			writeClassToRewriter(rewrite, currClass, methods, classVariables);
		}
	}

	private void corelateMethodsWithComments(List<MethodDto> methods, List<LineCommentDto> comments) {
		for (MethodDto currMethod : methods) {
			for (LineCommentDto currComment : comments) {

				// method includes its javadoc, -1 because we are looking for
				// the line above
				int lineRangeStart = currMethod.getMethodLineNumber() - 1;

				// body is the last line of the method header (containing the
				// <pre>{</pre> block start)
				int lineRangeEnd = currMethod.getBodyLineNumber() - 1;

				int commentLine = currComment.getLineNumber();

				if (commentLine >= lineRangeStart && commentLine <= lineRangeEnd) {
					currMethod.setAnnontation(getMethodAnnontationFromComment(currComment.getComment()));
				}
			}
		}
	}

	private void writeClassToRewriter(ASTRewrite rewrite, AbstractTypeDeclaration currClass, List<MethodDto> methods,
			List<FieldDeclaration> classVariables) {
		AST ast = currClass.getAST();

		addClassVariableAsFirstIfMissing(EeLogConstants.getQNameLogger(ast), EeLogConstants.VARIABLE_NAME_LOGGER,
				classVariables, StatementHelper.createLoggerStatement(ast), ast, rewrite, currClass);
		addClassVariableAsFirstIfMissing(EeLogConstants.getQNameString(ast), EeLogConstants.CONST_NAME_LOG_CLASS,
				classVariables, StatementHelper.createClassNameStringLiteral(currClass, ast), ast, rewrite, currClass);
		addClassVariableAsFirstIfMissing(EeLogConstants.getQNameLevel(ast), EeLogConstants.CONST_NAME_DEFAULT_LEVEL,
				classVariables, StatementHelper.createLogLevelStatement(Level.FINER, ast), ast, rewrite, currClass);

		writeMethodsToRewriter(methods, rewrite, ast);
	}

	private void addClassVariableAsFirstIfMissing(QualifiedName qualifiedClassName, String variableName,
			List<FieldDeclaration> classVariables, Expression value, AST ast, ASTRewrite rewriter,
			AbstractTypeDeclaration currClass) {
		for (FieldDeclaration currVar : classVariables) {
			Object firstFragment = StatementHelper.getFirstStatement(currVar);
			if (firstFragment instanceof VariableDeclarationFragment) {
				VariableDeclarationFragment fragment = (VariableDeclarationFragment) firstFragment;
				if (fragment.getName().getIdentifier().equals(variableName) && currVar.getType().resolveBinding()
						.getQualifiedName().equals(qualifiedClassName.getFullyQualifiedName())) {
					return;
				}
			}
		}
		writeClassFieldsToRewriter(qualifiedClassName, variableName, value, ast, rewriter, currClass);
	}

	private void writeClassFieldsToRewriter(QualifiedName qualifiedClassName, String variableName,
			Expression initializerExpression, AST ast, ASTRewrite rewriter, AbstractTypeDeclaration currClass) {
		System.out.println(
				"need to add a " + qualifiedClassName + " called " + variableName + " of " + initializerExpression);
		ListRewrite listRewrite = rewriter.getListRewrite(currClass, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);

		VariableDeclarationFragment declarationFragment = ast.newVariableDeclarationFragment();
		declarationFragment.setName(ast.newSimpleName(variableName));
		declarationFragment.setInitializer(initializerExpression);
		FieldDeclaration fieldDeclaration = ast.newFieldDeclaration(declarationFragment);
		fieldDeclaration.setType(ast.newSimpleType(ast.newName(qualifiedClassName.getName().getIdentifier())));
		listRewrite.insertFirst(fieldDeclaration, null);
	}

	private void writeMethodsToRewriter(List<MethodDto> methods, ASTRewrite rewrite, AST ast) {
		for (MethodDto currMethod : methods) {
			Block methodBlock = currMethod.getMethodBlock();
			ListRewrite listRewrite = rewrite.getListRewrite(methodBlock, Block.STATEMENTS_PROPERTY);

			if (currMethod.getMethodState() == MethodStateEnum.MISSING
					&& currMethod.getAnnontation() != MethodAnnotationEnum.OFF) {
				VariableDeclarationStatement methodNameStmt = StatementHelper.createMethodNameStatement(currMethod,
						ast);
				VariableDeclarationStatement isLoggingStmt = StatementHelper.createIsLoggingStatement(ast);
				IfStatement entryStmt = StatementHelper.getEntryLoggingStatement(ast);
				IfStatement exitStmt;

				listRewrite.insertFirst(entryStmt, null);
				listRewrite.insertFirst(isLoggingStmt, null);
				listRewrite.insertFirst(methodNameStmt, null);

				List originalStatements = listRewrite.getOriginalList();

				if(lastStatementIsReturnStatement(originalStatements)) {
					ReturnStatement returnStatement = (ReturnStatement) originalStatements.get(originalStatements.size() - 1);
					Expression returnExpression = returnStatement.getExpression();
					exitStmt = StatementHelper.getExitingLoggingStatement(ast, returnExpression);
					listRewrite.insertBefore(exitStmt, returnStatement, null);
				} else {
					exitStmt = StatementHelper.getExitingLoggingStatement(ast, null);
					listRewrite.insertLast(exitStmt, null);
				}
			} else if (currMethod.getMethodState() == MethodStateEnum.WRONG_SIGNATURE) {
				VariableDeclarationStatement firstStatement = StatementHelper
						.getFirstVariableDeclarationStatementOfBlock(methodBlock);
				VariableDeclarationStatement replacementStatement = StatementHelper
						.createMethodNameStatement(currMethod, ast);
				listRewrite.replace(firstStatement, replacementStatement, null);
			}
		}
	}

	private boolean lastStatementIsReturnStatement(List originalStatements) {
		if (originalStatements.size() >= 1) {
			Object lastStatement = originalStatements.get(originalStatements.size() - 1);
			if (lastStatement instanceof ReturnStatement) {
				return true;
			}
		}
		return false;
	}

	private void addLoggerImportToCompUnit(CompilationUnit parsedCompilationUnit, AST ast, ASTRewrite rewriter) {
		QualifiedName[] importClasses = { EeLogConstants.getQNameLogger(ast), EeLogConstants.getQNameLevel(ast) };

		ListRewrite listRewrite = rewriter.getListRewrite(parsedCompilationUnit, CompilationUnit.IMPORTS_PROPERTY);
		List<?> originalList = listRewrite.getOriginalList();

		for (QualifiedName importClass : importClasses) {
			if (!hasImportAlready(importClass, originalList)) {
				ImportDeclaration loggerImport = ast.newImportDeclaration();
				loggerImport.setName(importClass);
				parsedCompilationUnit.imports().add(loggerImport);
				listRewrite.insertFirst(loggerImport, null);
			}
		}
	}

	private boolean hasImportAlready(QualifiedName importClass, List<?> originalList) {
		for (Object object : originalList) {
			if (object instanceof ImportDeclaration) {
				ImportDeclaration currImportDeclaration = (ImportDeclaration) object;
				if (currImportDeclaration.getName().getFullyQualifiedName()
						.equals(importClass.getFullyQualifiedName())) {
					return true;
				}
			}
		}
		return false;
	}

	private static CompilationUnit parse(ICompilationUnit unit) {
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(unit);
		parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null);
	}

	private MethodAnnotationEnum getMethodAnnontationFromComment(String comment) {
		if (comment != null) {
			for (MethodAnnotationEnum currEnum : MethodAnnotationEnum.values()) {
				if (comment.equals(currEnum.getVerb())) {
					return currEnum;
				}
			}
		}
		return MethodAnnotationEnum.NONE;
	}

}