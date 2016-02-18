package de.anpross.eeloghelper.handlers;

import java.util.List;

import javax.inject.Inject;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.texteditor.ITextEditor;

import de.anpross.eeloghelper.EeLogConstants;
import de.anpross.eeloghelper.LoggerMethodMacher;
import de.anpross.eeloghelper.LoggerMethodMacher.MethodMatcher;
import de.anpross.eeloghelper.ParsingHelper;
import de.anpross.eeloghelper.StatementHelper;
import de.anpross.eeloghelper.dtos.ClassUpdateResultDto;
import de.anpross.eeloghelper.enums.LogStyleEnum;
import de.anpross.eeloghelper.visitors.ClassUpdateVisitor;

public class UpdateLoggingInCompilationUnitHandler extends AbstractHandler {
	@Inject
	IEclipseContext diContext;

	ParsingHelper parsingHelper;
	LoggerMethodMacher loggerMatcher;

	public UpdateLoggingInCompilationUnitHandler() {
		parsingHelper = ContextInjectionFactory.make(ParsingHelper.class, diContext);
		loggerMatcher = ContextInjectionFactory.make(LoggerMethodMacher.class, diContext);
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		ITextEditor currEditor = parsingHelper.getCurrEditor();
		parsingHelper.getAndStoreCurrentSelection(currEditor);
		ICompilationUnit compilationUnit = parsingHelper.getCurrenEditorsCompUnit(currEditor);

		updateCompilationUnit(compilationUnit);
		parsingHelper.moveToStoredEditorPos(currEditor);
		return null;
	}

	private void updateCompilationUnit(ICompilationUnit compilationUnit) throws ExecutionException {
		CompilationUnit parsedCompilationUnit = parsingHelper.parse(compilationUnit);
		List<AbstractTypeDeclaration> classes = parsedCompilationUnit.types();
		ASTRewrite rewrite = ASTRewrite.create(parsedCompilationUnit.getAST());
		for (AbstractTypeDeclaration currClass : classes) {
			updateClass(rewrite, currClass, parsedCompilationUnit);
		}

		try {
			TextEdit edits;

			edits = rewrite.rewriteAST();
			Document document = new Document(compilationUnit.getSource());
			edits.apply(document, TextEdit.UPDATE_REGIONS);

			compilationUnit.getBuffer().setContents(document.get());
		} catch (Exception e) {
			throw new ExecutionException("error writing to editor", e);
		}

	}

	private void updateClass(ASTRewrite rewrite, AbstractTypeDeclaration currClass, CompilationUnit parsedCompilationUnit) {
		ClassUpdateVisitor visitor = new ClassUpdateVisitor(EeLogConstants.getLogger());
		parsedCompilationUnit.accept(visitor);

		List<ClassUpdateResultDto> methods = visitor.getMethods();
		System.out.println(methods);
		for (ClassUpdateResultDto methodInvocation : methods) {
			LoggerMethodMacher.MethodMatcher methodMatcherIfMatching = loggerMatcher.getMethodMatcherIfMatching(methodInvocation);
			AST ast = parsedCompilationUnit.getAST();
			if (methodMatcherIfMatching != null) {
				Expression callExpression = StatementHelper.generateCallExpression(methodMatcherIfMatching.getCallParameters(), ast);
				fixLogMethod(methodMatcherIfMatching, methodInvocation.getSignature(), methodInvocation.getLogStyle(), callExpression,
						rewrite, ast);
			}
		}
	}

	private void fixLogMethod(MethodMatcher methodMatcher, String methodSignature, LogStyleEnum logStyle, Expression callExpression,
			ASTRewrite rewrite, AST ast) {
		System.out.println("fixing method: " + methodSignature + " with logstyle: " + logStyle);
		MethodInvocation originalInvocation = methodMatcher.getInvocation();
		MethodInvocation newInvocation = (MethodInvocation) ASTNode.copySubtree(ast, originalInvocation);

		// class
		SimpleName newLogClass = EeLogConstants.getLogClassName(ast);
		replaceArgument(newInvocation, methodMatcher.getClassParameterPos(), newLogClass);

		// method
		Expression methodNameExpression = getMethodExpression(methodSignature, logStyle, ast);
		replaceArgument(newInvocation, methodMatcher.getMethodParameterPos(), methodNameExpression);

		// entering
		if (methodMatcher.getPotentialCallParameterPos() != null) {
			// number of arguments might have changed, need a whole new method
			newInvocation = StatementHelper.createEntryLoggingInvocation(ast, callExpression, methodNameExpression);
		}

		// return
		if (methodMatcher.getPotentialReturnParameterPos() != null) {
			// number of arguments might have changed, need a whole new method
			newInvocation = StatementHelper.createExitingLoggingInvocation(ast, methodMatcher.getReturnExpression(), methodNameExpression);
		}
		rewrite.replace(originalInvocation, newInvocation, null);

	}

	private Expression getMethodExpression(String methodSignature, LogStyleEnum logStyle, AST ast) {
		Expression methodNameExpression;
		if (logStyle == LogStyleEnum.USE_LITERAL) {
			StringLiteral methodSigLiteral = ast.newStringLiteral();
			methodSigLiteral.setLiteralValue(methodSignature);
			methodNameExpression = methodSigLiteral;
		} else {
			methodNameExpression = EeLogConstants.getLogMethodName(ast);
		}
		return methodNameExpression;
	}

	private void replaceArgument(MethodInvocation newInvocation, int argumentIndex, Expression newLogClass) {
		newInvocation.arguments().remove(argumentIndex);
		newInvocation.arguments().add(argumentIndex, newLogClass);
	}
}
