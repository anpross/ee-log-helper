package de.anpross.eeloghelper;

import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ITypeRoot;
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
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;

import de.anpross.eeloghelper.dtos.AnnotatatedItem;
import de.anpross.eeloghelper.dtos.ClassDto;
import de.anpross.eeloghelper.dtos.LineCommentDto;
import de.anpross.eeloghelper.dtos.MethodDto;
import de.anpross.eeloghelper.enums.CallTypeAnnotationEnum;
import de.anpross.eeloghelper.enums.DefaultBehaviorEnum;
import de.anpross.eeloghelper.enums.MethodAnnotationEnum;
import de.anpross.eeloghelper.enums.MethodStateEnum;
import de.anpross.eeloghelper.visitors.ClassVisitor;
import de.anpross.eeloghelper.visitors.LineCommentVisitor;

@SuppressWarnings("unchecked")
public class EeLogHandler extends AbstractHandler {

	private static final String ANNOTATION_DELIMITER = " ";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		ITextEditor editor = (ITextEditor) HandlerUtil.getActiveEditor(event);
        ITypeRoot typeRoot = JavaUI.getEditorInputTypeRoot(editor.getEditorInput());
        ICompilationUnit compilationUnit = (ICompilationUnit) typeRoot.getAdapter(ICompilationUnit.class);
        
		try {
			createAST(compilationUnit);
		} catch (JavaModelException | MalformedTreeException
				| BadLocationException e) {
			e.printStackTrace();
		}
		return null;
	}


	private void createAST(ICompilationUnit unit) throws JavaModelException, MalformedTreeException, BadLocationException {
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

	private void processClass(ASTRewrite rewrite, AbstractTypeDeclaration currClass, CompilationUnit parsedCompilationUnit,
			String[] compilationUnitSource) {
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

		corelateMethodsWithComments(methods, comments);
		corelateClassWithComments(classDto, comments);

		if (!methods.isEmpty()) {
			writeClassToRewriter(rewrite, currClass, methods, classVariables, classDto);
		}
	}

	private void corelateClassWithComments(ClassDto classDto, List<LineCommentDto> comments) {
		String matchingCommentString = getMatchingCommentString(comments, classDto);
		classDto.setCallTypeAnnotation(getCallTypeAnnontationFromComment(matchingCommentString));
		classDto.setDefaultBehaviorEnum(getDefaultBehaviorAnnontationFromComment(matchingCommentString));
	}

	private void corelateMethodsWithComments(List<MethodDto> methods, List<LineCommentDto> comments) {
		for (MethodDto currMethod : methods) {
			String matchingCommentString = getMatchingCommentString(comments, currMethod);
			currMethod.setAnnotation(getMethodAnnontationFromComment(matchingCommentString));
		}
	}

	private String getMatchingCommentString(List<LineCommentDto> comments, AnnotatatedItem currMethod) {
		for (LineCommentDto currComment : comments) {

			// method includes its javadoc, -1 because we are looking for the line above
			int lineRangeStart = currMethod.getSignatureLineNumber() - 1;

			// body is the last line of the method header (containing the <pre>{</pre> block start)
			int lineRangeEnd = currMethod.getBodyLineNumber() - 1;

			int commentLine = currComment.getLineNumber();

			if (commentLine >= lineRangeStart && commentLine <= lineRangeEnd) {
				return currComment.getComment();
			}
		}
		return null;
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

			boolean shouldLog = isLoggingRequired(currMethod, currClass);
			if (currMethod.getMethodState() == MethodStateEnum.MISSING && shouldLog) {
				VariableDeclarationStatement methodNameStmt = StatementHelper.createMethodNameStatement(currMethod, ast);
				listRewrite.insertFirst(methodNameStmt, null);

				VariableDeclarationStatement isLoggingStmt = StatementHelper.createIsLoggingStatement(ast);
				listRewrite.insertAfter(isLoggingStmt, methodNameStmt, null);

				IfStatement entryStmt = StatementHelper.getEntryLoggingStatement(ast);
				listRewrite.insertAfter(entryStmt, isLoggingStmt, null);

				insertExitLogStatement(ast, listRewrite);
			} else if (currMethod.getMethodState() == MethodStateEnum.WRONG_SIGNATURE) {
				VariableDeclarationStatement firstStatement = StatementHelper.getFirstVariableDeclarationStatementOfBlock(methodBlock);
				VariableDeclarationStatement replacementStatement = StatementHelper.createMethodNameStatement(currMethod, ast);
				listRewrite.replace(firstStatement, replacementStatement, null);
			}
		}
	}

	private boolean isLoggingRequired(MethodDto currMethod, ClassDto currClass) {
		boolean defaultOn = currClass.getDefaultBehaviorEnum().equals(DefaultBehaviorEnum.DEFAULT_ON);
		boolean methodExplOff = currMethod.getAnnotation().equals(MethodAnnotationEnum.OFF);
		boolean methodExplOn = currMethod.getAnnotation().equals(MethodAnnotationEnum.ON);
		if (methodExplOff) {
			return false;
		} else if (methodExplOn) {
			return true;
		} else if (defaultOn) {
			return true;
		} else {
			return false;
		}
	}

	private void insertExitLogStatement(AST ast, ListRewrite listRewrite) {
		List<?> originalStatements = listRewrite.getOriginalList();
		if (lastStatementIsReturnStatement(originalStatements)) {
			ReturnStatement returnStatement = (ReturnStatement) originalStatements.get(originalStatements.size() - 1);
			Expression returnExpression = returnStatement.getExpression();
			IfStatement exitStmt = StatementHelper.getExitingLoggingStatement(ast, returnExpression);
			listRewrite.insertBefore(exitStmt, returnStatement, null);
		} else {
			IfStatement exitStmt = StatementHelper.getExitingLoggingStatement(ast, null);
			listRewrite.insertLast(exitStmt, null);
		}
	}

	private boolean lastStatementIsReturnStatement(List<?> originalStatements) {
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
				if (currImportDeclaration.getName().getFullyQualifiedName().equals(importClass.getFullyQualifiedName())) {
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

	private CallTypeAnnotationEnum getCallTypeAnnontationFromComment(String comment) {
		if (comment != null) {
			StringTokenizer tokenizer = new StringTokenizer(comment, ANNOTATION_DELIMITER);
			while (tokenizer.hasMoreTokens()) {
				String currToken = tokenizer.nextToken();
				for (CallTypeAnnotationEnum currEnum : CallTypeAnnotationEnum.values()) {
					if (currToken.equals(currEnum.getVerb())) {
						return currEnum;
					}
				}
			}
		}
		return CallTypeAnnotationEnum.NONE;
	}

	private DefaultBehaviorEnum getDefaultBehaviorAnnontationFromComment(String comment) {
		if (comment != null) {
			StringTokenizer tokenizer = new StringTokenizer(comment, ANNOTATION_DELIMITER);
			while (tokenizer.hasMoreTokens()) {
				String currToken = tokenizer.nextToken();
				for (DefaultBehaviorEnum currEnum : DefaultBehaviorEnum.values()) {
					if (currToken.equals(currEnum.getVerb())) {
						return currEnum;
					}
				}
			}
		}
		return DefaultBehaviorEnum.DEFAULT_ON;
	}

}