package de.anpross.eeloghelper.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.inject.Inject;

import org.eclipse.core.commands.AbstractHandler;
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
import org.eclipse.jdt.core.dom.LineComment;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Type;
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

import de.anpross.eeloghelper.Activator;
import de.anpross.eeloghelper.EditorPositionBean;
import de.anpross.eeloghelper.EeLogConstants;
import de.anpross.eeloghelper.ParsingHelper;
import de.anpross.eeloghelper.PreferencePage;
import de.anpross.eeloghelper.StatementHelper;
import de.anpross.eeloghelper.dtos.ClassDto;
import de.anpross.eeloghelper.dtos.LineCommentDto;
import de.anpross.eeloghelper.dtos.MethodDto;
import de.anpross.eeloghelper.enums.CallTypeAnnotationEnum;
import de.anpross.eeloghelper.enums.LogStyleEnum;
import de.anpross.eeloghelper.enums.MethodStateEnum;
import de.anpross.eeloghelper.visitors.ClassVisitor;
import de.anpross.eeloghelper.visitors.LineCommentVisitor;

@SuppressWarnings("unchecked")
public abstract class AddLoggingHandler extends AbstractHandler {

	@Inject
	IEclipseContext diContext;

	@Inject
	ParsingHelper parsingHelper;

	@Inject
	EditorPositionBean editorPosition;

	public AddLoggingHandler() {
		parsingHelper = ContextInjectionFactory.make(ParsingHelper.class, diContext);
	}

	protected void processCompilationUnit(ICompilationUnit unit, IMethod currMethod)
			throws JavaModelException, MalformedTreeException, BadLocationException {
		ITextEditor currEditor = parsingHelper.getCurrEditor();
		parsingHelper.getAndStoreCurrentSelection(currEditor);

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
		parsingHelper.moveToStoredEditorPos(currEditor);
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

		// only current method usecase
		if (currMethod != null) {
			methods = parsingHelper.filterForCurrentMethod(methods, currMethod);
		}

		if (!methods.isEmpty()) {
			writeClassToRewriter(rewrite, currClass, methods, classVariables, classDto);
		}
	}

	/**
	 *
	 * @param rewrite
	 *            the ASTRewrite object
	 * @param currClass
	 *            the JDT DOM Class object
	 * @param methods
	 *            the methods which to add EE-logs to
	 * @param classVariables
	 *            required class variables
	 * @param classDto
	 *            class related information
	 */
	private void writeClassToRewriter(ASTRewrite rewrite, AbstractTypeDeclaration currClass, List<MethodDto> methods,
			List<FieldDeclaration> classVariables, ClassDto classDto) {
		AST ast = currClass.getAST();

		if (classDto.getCallTypeAnnotation().equals(CallTypeAnnotationEnum.PER_INSTANCE_EVAL)) {
			System.out.println("we are in PER-INSTANCE mode");
			Expression creteIsLoggingExpression = StatementHelper.createIsLoggingMethodInvocation(ast);
			addClassFieldAsFirstIfMissing(ast.newPrimitiveType(PrimitiveType.BOOLEAN), EeLogConstants.getIsLoggingName(ast), classVariables,
					creteIsLoggingExpression, ast, rewrite, currClass);
		}

		addClassFieldAsFirstIfMissing(EeLogConstants.getLoggerType(ast), EeLogConstants.getLoggerName(ast), classVariables,
				StatementHelper.createLoggerStatement(ast), ast, rewrite, currClass);
		addClassFieldAsFirstIfMissing(EeLogConstants.getTypeString(ast), EeLogConstants.getLogClassName(ast), classVariables,
				StatementHelper.createClassNameStringLiteral(currClass, ast), ast, rewrite, currClass);
		addClassFieldAsFirstIfMissing(EeLogConstants.getTypeLevel(ast), EeLogConstants.getDefaultLevelName(ast), classVariables,
				StatementHelper.createLogLevelStatement(Level.FINER, ast), ast, rewrite, currClass);

		writeMethodsToRewriter(methods, classDto, rewrite, ast);
	}

	private void addClassFieldAsFirstIfMissing(Type fieldType, Name fieldName, List<FieldDeclaration> classVariables, Expression value,
			AST ast, ASTRewrite rewriter, AbstractTypeDeclaration currClass) {
		for (FieldDeclaration currVar : classVariables) {
			Object firstFragment = StatementHelper.getFirstStatement(currVar);
			if (firstFragment instanceof VariableDeclarationFragment) {
				VariableDeclarationFragment fragment = (VariableDeclarationFragment) firstFragment;
				boolean typesMatch = parsingHelper.isTypesEqual(currVar.getType(), fieldType);
				boolean idendifierMatch = parsingHelper.isIdentifierEqual(fieldName, fragment.getName());
				if (idendifierMatch && typesMatch) {
					return;
				}
			}
		}
		writeClassFieldsToRewriter(fieldType, fieldName, value, ast, rewriter, currClass);
	}

	private void writeClassFieldsToRewriter(Type fieldType, Name fieldName, Expression initializerExpression, AST ast, ASTRewrite rewriter,
			AbstractTypeDeclaration currClass) {
		System.out.println("need to add a " + fieldType + " called " + fieldName + " of " + initializerExpression);
		ListRewrite listRewrite = rewriter.getListRewrite(currClass, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);

		FieldDeclaration fieldDeclaration = StatementHelper.createPrivateStaticField(fieldType, fieldName, initializerExpression, ast);
		listRewrite.insertFirst(fieldDeclaration, null);
	}

	private void writeMethodsToRewriter(List<MethodDto> methods, ClassDto currClass, ASTRewrite rewrite, AST ast) {
		String logStyleString = Activator.getDefault().getPreferenceStore().getString(PreferencePage.PREF_LOG_STYLE);
		System.out.println("logstyle: " + logStyleString);
		LogStyleEnum logStyle = LogStyleEnum.valueOf(logStyleString);

		for (MethodDto currMethod : methods) {
			Block methodBlock = currMethod.getMethodBlock();
			ListRewrite listRewrite = rewrite.getListRewrite(methodBlock, Block.STATEMENTS_PROPERTY);

			boolean shouldLog = parsingHelper.isLoggingRequired(currMethod, currClass);
			if (currMethod.getMethodState() == MethodStateEnum.MISSING && shouldLog) {
				List<Statement> statements = new ArrayList<Statement>();
				if (logStyle.equals(LogStyleEnum.USE_VARIABLE)) {
					VariableDeclarationStatement methodNameStmt = StatementHelper.createMethodNameStatement(currMethod, ast);
					statements.add(methodNameStmt);
				}

				if (currClass.getCallTypeAnnotation().getEffectiveMode() == CallTypeAnnotationEnum.PER_CALL_EVAL) {
					VariableDeclarationStatement isLoggingStmt = StatementHelper.createIsLoggingStatement(ast);
					statements.add(isLoggingStmt);
				}

				Expression callExpression = StatementHelper.generateCallExpression(currMethod.getMethodDeclaration().parameters(), ast);
				StatementHelper.insertEntryLogStatement(ast, logStyle, currMethod, statements, callExpression);

				StatementHelper.insertStatementsToListRewrite(listRewrite, statements);

				StatementHelper.insertExitLogStatement(ast, logStyle, currMethod, listRewrite);
			} else if (currMethod.getMethodState() == MethodStateEnum.WRONG_SIGNATURE) {
				VariableDeclarationStatement firstStatement = StatementHelper.getFirstVariableDeclarationStatementOfBlock(methodBlock);
				VariableDeclarationStatement replacementStatement = StatementHelper.createMethodNameStatement(currMethod, ast);
				listRewrite.replace(firstStatement, replacementStatement, null);
			}
		}
	}
}