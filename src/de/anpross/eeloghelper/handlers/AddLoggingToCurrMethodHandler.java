package de.anpross.eeloghelper.handlers;

import java.util.List;
import java.util.logging.Level;

import javax.inject.Inject;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.LineComment;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.texteditor.ITextEditor;

import de.anpross.eeloghelper.EeLogConstants;
import de.anpross.eeloghelper.ParsingHelper;
import de.anpross.eeloghelper.StatementHelper;
import de.anpross.eeloghelper.dtos.ClassDto;
import de.anpross.eeloghelper.dtos.LineCommentDto;
import de.anpross.eeloghelper.dtos.MethodDto;
import de.anpross.eeloghelper.enums.MethodStateEnum;
import de.anpross.eeloghelper.visitors.ClassVisitor;
import de.anpross.eeloghelper.visitors.LineCommentVisitor;

@SuppressWarnings("unchecked")
public class AddLoggingToCurrMethodHandler extends AbstractHandler {

	@Inject
	IEclipseContext context;

	ParsingHelper parsingHelper;

	public AddLoggingToCurrMethodHandler() {
		parsingHelper = ContextInjectionFactory.make(ParsingHelper.class, context);
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		ITextEditor currEditor = parsingHelper.getCurrEditor();
		ICompilationUnit compilationUnit = parsingHelper.getCurrenEditorsCompUnit(currEditor);
		IMethod currentMethod = parsingHelper.getCurrentMethod(currEditor, compilationUnit);

		try {
			processCompilationUnit(compilationUnit, currentMethod);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private void processCompilationUnit(ICompilationUnit unit, IMethod currMethod)
			throws JavaModelException, MalformedTreeException, BadLocationException {
		String[] compilationUnitSource = unit.getSource().split("\n");
		CompilationUnit parsedCompilationUnit = parsingHelper.parse(unit);
		List<AbstractTypeDeclaration> classes = parsedCompilationUnit.types();
		ASTRewrite rewrite = ASTRewrite.create(parsedCompilationUnit.getAST());
		parsingHelper.addLoggerImportToCompUnit(parsedCompilationUnit, parsedCompilationUnit.getAST(), rewrite);
		for (AbstractTypeDeclaration currClass : classes) {
			processClass(rewrite, currClass, parsedCompilationUnit, compilationUnitSource, currMethod);
		}
		TextEdit edits = rewrite.rewriteAST();
		Document document = new Document(unit.getSource());
		edits.apply(document);

		unit.getBuffer().setContents(document.get());
	}

	private void processClass(ASTRewrite rewrite, AbstractTypeDeclaration currClass, CompilationUnit parsedCompilationUnit,
			String[] compilationUnitSource, IMethod currMethod) {
		ClassVisitor classVisitor = new ClassVisitor(parsedCompilationUnit);
		currClass.accept(classVisitor);

		List<?> commentList = parsedCompilationUnit.getCommentList();
		// Comments need special care so they get sorta-picked up by the Vistor (comment content not included)
		LineCommentVisitor lineCommentVisitor = new LineCommentVisitor(parsedCompilationUnit, compilationUnitSource);
		for (Object object : commentList) {
			if (object instanceof LineComment) {
				LineComment currLine = (LineComment) object;
				currLine.accept(lineCommentVisitor);
			}
		}

		List<MethodDto> methods = classVisitor.getMethods();
		List<FieldDeclaration> classVariables = classVisitor.getClassVariables();
		ClassDto classDto = classVisitor.getClassDto();

		List<LineCommentDto> comments = lineCommentVisitor.getMethodComments();

		parsingHelper.corelateMethodsWithComments(methods, comments);
		parsingHelper.corelateClassWithComments(classDto, comments);

		methods = parsingHelper.filterForCurrentMethod(methods, currMethod);

		if (!methods.isEmpty()) {
			writeClassToRewriter(rewrite, currClass, methods, classVariables, classDto);
		}
	}

	private void writeClassToRewriter(ASTRewrite rewrite, AbstractTypeDeclaration currClass, List<MethodDto> methods,
			List<FieldDeclaration> classVariables, ClassDto classDto) {
		AST ast = currClass.getAST();

		addClassVariableAsFirstIfMissing(EeLogConstants.getQNameLogger(ast), EeLogConstants.VARIABLE_NAME_LOGGER, classVariables,
				StatementHelper.createLoggerStatement(ast), ast, rewrite, currClass);
		addClassVariableAsFirstIfMissing(EeLogConstants.getQNameString(ast), EeLogConstants.CONST_NAME_LOG_CLASS, classVariables,
				StatementHelper.createClassNameStringLiteral(currClass, ast), ast, rewrite, currClass);
		addClassVariableAsFirstIfMissing(EeLogConstants.getQNameLevel(ast), EeLogConstants.CONST_NAME_DEFAULT_LEVEL, classVariables,
				StatementHelper.createLogLevelStatement(Level.FINER, ast), ast, rewrite, currClass);

		writeMethodsToRewriter(methods, classDto, rewrite, ast);
	}

	private void addClassVariableAsFirstIfMissing(QualifiedName qualifiedClassName, String variableName,
			List<FieldDeclaration> classVariables, Expression value, AST ast, ASTRewrite rewriter, AbstractTypeDeclaration currClass) {
		for (FieldDeclaration currVar : classVariables) {
			Object firstFragment = StatementHelper.getFirstStatement(currVar);
			if (firstFragment instanceof VariableDeclarationFragment) {
				VariableDeclarationFragment fragment = (VariableDeclarationFragment) firstFragment;
				boolean typesMatch = currVar.getType().resolveBinding().getQualifiedName()
						.equals(qualifiedClassName.getFullyQualifiedName());
				boolean idendifierMatch = fragment.getName().getIdentifier().equals(variableName);
				if (idendifierMatch && typesMatch) {
					return;
				}
			}
		}
		writeClassFieldsToRewriter(qualifiedClassName, variableName, value, ast, rewriter, currClass);
	}

	private void writeClassFieldsToRewriter(QualifiedName qualifiedClassName, String variableName, Expression initializerExpression,
			AST ast, ASTRewrite rewriter, AbstractTypeDeclaration currClass) {
		System.out.println("need to add a " + qualifiedClassName + " called " + variableName + " of " + initializerExpression);
		ListRewrite listRewrite = rewriter.getListRewrite(currClass, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);

		VariableDeclarationFragment declarationFragment = ast.newVariableDeclarationFragment();
		declarationFragment.setName(ast.newSimpleName(variableName));
		declarationFragment.setInitializer(initializerExpression);
		FieldDeclaration fieldDeclaration = ast.newFieldDeclaration(declarationFragment);
		fieldDeclaration.setType(ast.newSimpleType(ast.newName(qualifiedClassName.getName().getIdentifier())));
		listRewrite.insertFirst(fieldDeclaration, null);
	}

	private void writeMethodsToRewriter(List<MethodDto> methods, ClassDto currClass, ASTRewrite rewrite, AST ast) {
		for (MethodDto currMethod : methods) {
			Block methodBlock = currMethod.getMethodBlock();
			ListRewrite listRewrite = rewrite.getListRewrite(methodBlock, Block.STATEMENTS_PROPERTY);

			boolean shouldLog = parsingHelper.isLoggingRequired(currMethod, currClass);
			if (currMethod.getMethodState() == MethodStateEnum.MISSING && shouldLog) {
				VariableDeclarationStatement methodNameStmt = StatementHelper.createMethodNameStatement(currMethod, ast);
				listRewrite.insertFirst(methodNameStmt, null);

				VariableDeclarationStatement isLoggingStmt = StatementHelper.createIsLoggingStatement(ast);
				listRewrite.insertAfter(isLoggingStmt, methodNameStmt, null);

				IfStatement entryStmt = StatementHelper.createEntryLoggingStatement(ast);
				listRewrite.insertAfter(entryStmt, isLoggingStmt, null);

				parsingHelper.insertExitLogStatement(ast, listRewrite);
			} else if (currMethod.getMethodState() == MethodStateEnum.WRONG_SIGNATURE) {
				VariableDeclarationStatement firstStatement = StatementHelper.getFirstVariableDeclarationStatementOfBlock(methodBlock);
				VariableDeclarationStatement replacementStatement = StatementHelper.createMethodNameStatement(currMethod, ast);
				listRewrite.replace(firstStatement, replacementStatement, null);
			}
		}
	}
}