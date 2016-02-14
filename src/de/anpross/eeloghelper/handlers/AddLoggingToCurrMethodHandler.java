package de.anpross.eeloghelper.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.ui.texteditor.ITextEditor;

@SuppressWarnings("unchecked")
public class AddLoggingToCurrMethodHandler extends AddLoggingHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		ITextEditor currEditor = parsingHelper.getCurrEditor();
		ICompilationUnit compilationUnit = parsingHelper.getCurrenEditorsCompUnit(currEditor);
		IMethod currentMethod = parsingHelper.getCurrentMethod(currEditor, compilationUnit);

		System.out.println("adding logging to current method");

		try {
			processCompilationUnit(compilationUnit, currentMethod);
		} catch (Exception e) {
			e.printStackTrace();
		}
		parsingHelper.moveToStoredEditorPos(currEditor);
		return null;
	}
}