package de.anpross.eeloghelper.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.ui.texteditor.ITextEditor;

@SuppressWarnings("unchecked")
public class AddLoggingToCurrCompilationUnitHandler extends AddLoggingHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		ITextEditor currEditor = parsingHelper.getCurrEditor();
		ICompilationUnit compilationUnit = parsingHelper.getCurrenEditorsCompUnit(currEditor);

		System.out.println("adding logging to current comilation unit (aka File)");

		try {
			processCompilationUnit(compilationUnit, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		parsingHelper.moveToStoredEditorPos(currEditor);
		return null;
	}
}