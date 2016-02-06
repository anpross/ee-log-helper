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
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import de.anpross.eeloghelper.enums.MethodStateEnum;

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
			// now create the AST for the ICompilationUnits
			CompilationUnit parsedCompilationUnit = parse(unit);
			List<AbstractTypeDeclaration> classes = (List<AbstractTypeDeclaration>) parsedCompilationUnit.types();
			ASTRewrite rewrite = ASTRewrite.create(parsedCompilationUnit.getAST());
			addLoggerImportToCompUnit(parsedCompilationUnit, parsedCompilationUnit.getAST(), rewrite);
			for (AbstractTypeDeclaration currClass : classes) {
				processClass(rewrite, currClass);
			}
			TextEdit edits = rewrite.rewriteAST();
			Document document = new Document(unit.getSource());
			edits.apply(document);

			unit.getBuffer().setContents(document.get());
		}
	}

	private void processClass(ASTRewrite rewrite, AbstractTypeDeclaration currClass) {
		EntryExitLogVisitor visitor = new EntryExitLogVisitor();
		currClass.accept(visitor);

		List<MethodDto> methods = visitor.getMethods();
		List<FieldDeclaration> classVariables = visitor.getClassVariables();

		if (!methods.isEmpty()) {
			writeClassToRewriter(rewrite, currClass, methods, classVariables);
		}
	}

	private void writeClassToRewriter(ASTRewrite rewrite, AbstractTypeDeclaration currClass, List<MethodDto> methods,
			List<FieldDeclaration> classVariables) {
		AST ast = currClass.getAST();

		addClassVariableIfMissingAsFirst("Logger", "LOGGER", classVariables,
				StatementHelper.createLoggerStatement(ast), ast, rewrite, currClass);
		addClassVariableIfMissingAsFirst("String", "LOG_CLASS", classVariables,
				StatementHelper.createClassNameStringLiteral(currClass, ast), ast, rewrite, currClass);
		addClassVariableIfMissingAsFirst("Level", "DEFAULT_LEVEL", classVariables,
				StatementHelper.createLogLevelStatement(Level.FINER, ast), ast, rewrite, currClass);

		writeMethodsToRewriter(methods, rewrite, ast);
	}

	private void addClassVariableIfMissingAsFirst(String qualifiedClassName, String variableName,
			List<FieldDeclaration> classVariables, Expression value, AST ast, ASTRewrite rewriter,
			AbstractTypeDeclaration currClass) {
		for (FieldDeclaration currVar : classVariables) {
			List fragments = currVar.fragments();
			if (fragments.size() == 1) {
				Object firstFragment = fragments.get(0);
				if (firstFragment instanceof VariableDeclarationFragment) {
					VariableDeclarationFragment fragment = (VariableDeclarationFragment) firstFragment;
					if (fragment.getName().getIdentifier().equals(variableName)
							&& currVar.getType().resolveBinding().getQualifiedName().equals(qualifiedClassName)) {
						// TODO: remove existing entry so we can re-add it in
						// case it changed
						return;
					}
				}
			}
		}
		writeClassFieldsToRewriter(qualifiedClassName, variableName, value, ast, rewriter, currClass);
	}

	private void writeClassFieldsToRewriter(String qualifiedClassName, String variableName,
			Expression initializerExpression, AST ast, ASTRewrite rewriter, AbstractTypeDeclaration currClass) {
		System.out.println(
				"need to add a " + qualifiedClassName + " called " + variableName + " of " + initializerExpression);
		ListRewrite listRewrite = rewriter.getListRewrite(currClass, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);

		VariableDeclarationFragment declarationFragment = ast.newVariableDeclarationFragment();
		declarationFragment.setName(ast.newSimpleName(variableName));
		declarationFragment.setInitializer(initializerExpression);
		FieldDeclaration fieldDeclaration = ast.newFieldDeclaration(declarationFragment);
		fieldDeclaration.setType(ast.newSimpleType(ast.newName(qualifiedClassName)));
		listRewrite.insertFirst(fieldDeclaration, null);
	}

	private void writeMethodsToRewriter(List<MethodDto> methods, ASTRewrite rewrite, AST ast) {
		for (MethodDto currMethod : methods) {
			if (currMethod.getMethodState() == MethodStateEnum.MISSING) {
				VariableDeclarationStatement methodNameStmt = StatementHelper.createMethodNameStatement(currMethod,
						ast);
				VariableDeclarationStatement isLoggingStmt = StatementHelper.createIsLoggingStatement(ast);
				IfStatement entryStmt = StatementHelper.getEntryLoggingStatement(ast);
				IfStatement exitStmt = StatementHelper.getExitingLoggingStatement(ast);
				ListRewrite listRewrite = rewrite.getListRewrite(currMethod.getMethodBlock(),
						Block.STATEMENTS_PROPERTY);

				listRewrite.insertFirst(entryStmt, null);
				listRewrite.insertFirst(isLoggingStmt, null);
				listRewrite.insertFirst(methodNameStmt, null);
				listRewrite.insertLast(exitStmt, null);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void addLoggerImportToCompUnit(CompilationUnit parsedCompilationUnit, AST ast, ASTRewrite rewriter) {
		String[] importClasses = { "java.util.logging.Logger", "java.util.logging.Level" };

		ListRewrite listRewrite = rewriter.getListRewrite(parsedCompilationUnit, CompilationUnit.IMPORTS_PROPERTY);
		for (String importClass : importClasses) {
			ImportDeclaration loggerImport = ast.newImportDeclaration();
			loggerImport.setName(ast.newName(importClass));
			parsedCompilationUnit.imports().add(loggerImport);
			listRewrite.insertFirst(loggerImport, null);
		}
	}

	private static CompilationUnit parse(ICompilationUnit unit) {
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(unit);
		parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null); // parse
	}
}