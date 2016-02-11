package de.anpross.eeloghelper.handlers;

import java.util.List;

import javax.inject.Inject;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.ui.texteditor.ITextEditor;

import de.anpross.eeloghelper.EeLogConstants;
import de.anpross.eeloghelper.LoggerMethodMacher;
import de.anpross.eeloghelper.LoggerMethodMacher.MethodMatcher;
import de.anpross.eeloghelper.ParsingHelper;
import de.anpross.eeloghelper.visitors.ClassUpdateVisitor;

public class UpdateAllLogStatementsInClassHandler extends AbstractHandler {
	@Inject
	IEclipseContext context;

	ParsingHelper parsingHelper;
	LoggerMethodMacher loggerMatcher;

	public UpdateAllLogStatementsInClassHandler() {
		parsingHelper = ContextInjectionFactory.make(ParsingHelper.class, context);
		loggerMatcher = ContextInjectionFactory.make(LoggerMethodMacher.class, context);

	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		ITextEditor currEditor = parsingHelper.getCurrEditor();
		ICompilationUnit compilationUnit = parsingHelper.getCurrenEditorsCompUnit(currEditor);

		updateCompilationUnit(compilationUnit);
		return null;
	}

	private void updateCompilationUnit(ICompilationUnit compilationUnit) {
		CompilationUnit parsedCompilationUnit = parsingHelper.parse(compilationUnit);
		List<AbstractTypeDeclaration> classes = parsedCompilationUnit.types();
		ASTRewrite rewrite = ASTRewrite.create(parsedCompilationUnit.getAST());
		for (AbstractTypeDeclaration currClass : classes) {
			updateClass(rewrite, currClass, parsedCompilationUnit);
		}
	}

	private void updateClass(ASTRewrite rewrite, AbstractTypeDeclaration currClass, CompilationUnit parsedCompilationUnit) {
		ClassUpdateVisitor visitor = new ClassUpdateVisitor(EeLogConstants.VARIABLE_NAME_LOGGER);
		parsedCompilationUnit.accept(visitor);

		List<MethodInvocation> methods = visitor.getMethods();
		System.out.println(methods);
		for (MethodInvocation methodInvocation : methods) {
			LoggerMethodMacher.MethodMatcher methodMatcherIfMatching = loggerMatcher.getMethodMatcherIfMatching(methodInvocation);
			if (methodMatcherIfMatching != null) {
				fixLogMethod(methodMatcherIfMatching);
			}
		}
	}

	private void fixLogMethod(MethodMatcher methodMatcherIfMatching) {
		// TODO Auto-generated method stub

	}

}
