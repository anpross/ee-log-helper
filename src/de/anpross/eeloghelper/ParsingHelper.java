package de.anpross.eeloghelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import de.anpross.eeloghelper.dtos.AnnotatatedItem;
import de.anpross.eeloghelper.dtos.ClassDto;
import de.anpross.eeloghelper.dtos.LineCommentDto;
import de.anpross.eeloghelper.dtos.MethodDto;
import de.anpross.eeloghelper.enums.CallTypeAnnotationEnum;
import de.anpross.eeloghelper.enums.DefaultBehaviorEnum;
import de.anpross.eeloghelper.enums.MethodAnnotationEnum;

public class ParsingHelper {

	private static final String ANNOTATION_DELIMITER = " ";

	public ITextEditor getCurrEditor() {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		return (ITextEditor) page.getActiveEditor();
	}

	public ICompilationUnit getCurrenEditorsCompUnit(ITextEditor editor) {
		IJavaElement elem = JavaUI.getEditorInputJavaElement(editor.getEditorInput());
		if (elem instanceof ICompilationUnit) {
			return (ICompilationUnit) elem;
		}
		return null;
	}

	public IMethod getCurrentMethod(ITextEditor editor, ICompilationUnit compUnit) throws ExecutionException {
		ITextSelection sel = (ITextSelection) editor.getSelectionProvider().getSelection();
		IJavaElement selected;
		try {
			selected = compUnit.getElementAt(sel.getOffset());
		} catch (JavaModelException e) {
			throw new ExecutionException("JavaModelException", e);
		}
		if (selected != null && selected.getElementType() == IJavaElement.METHOD) {
			return (IMethod) selected;
		}
		return null;
	}

	public void corelateClassWithComments(ClassDto classDto, List<LineCommentDto> comments) {
		String matchingCommentString = getMatchingCommentString(comments, classDto);
		classDto.setCallTypeAnnotation(getCallTypeAnnontationFromComment(matchingCommentString));
		classDto.setDefaultBehaviorEnum(getDefaultBehaviorAnnontationFromComment(matchingCommentString));
	}

	public void corelateMethodsWithComments(List<MethodDto> methods, List<LineCommentDto> comments) {
		for (MethodDto currMethod : methods) {
			String matchingCommentString = getMatchingCommentString(comments, currMethod);
			currMethod.setAnnotation(getMethodAnnontationFromComment(matchingCommentString));
		}
	}

	public List<MethodDto> filterForCurrentMethod(List<MethodDto> methods, IMethod currMethod) {
		List<MethodDto> filteredList = new ArrayList<MethodDto>();
		for (Iterator<MethodDto> iterator = methods.iterator(); iterator.hasNext();) {
			MethodDto methodDto = iterator.next();
			IMethod methodDtoElement = (IMethod) methodDto.getMethodDeclaration().resolveBinding().getJavaElement();
			if (methodDtoElement != null && methodDtoElement.equals(currMethod)) {
				filteredList.add(methodDto);
				System.out.println("found method" + methodDto);
			}
		}
		return filteredList;
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

	public boolean isLoggingRequired(MethodDto currMethod, ClassDto currClass) {
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

	public void insertExitLogStatement(AST ast, ListRewrite listRewrite) {
		List<?> originalStatements = listRewrite.getOriginalList();
		if (lastStatementIsReturnStatement(originalStatements)) {
			ReturnStatement returnStatement = (ReturnStatement) originalStatements.get(originalStatements.size() - 1);
			Expression returnExpression = returnStatement.getExpression();
			IfStatement exitStmt = StatementHelper.createExitingLoggingIfStatement(ast, returnExpression);
			listRewrite.insertBefore(exitStmt, returnStatement, null);
		} else {
			IfStatement exitStmt = StatementHelper.createExitingLoggingIfStatement(ast, null);
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

	public void addLoggerImportToCompUnit(CompilationUnit parsedCompilationUnit, AST ast, ASTRewrite rewriter) {
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

	public CompilationUnit parse(ICompilationUnit unit) {
		@SuppressWarnings("deprecation") // this needs to work in eclipse 4.2
		ASTParser parser = ASTParser.newParser(AST.JLS4);
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
