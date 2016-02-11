package de.anpross.eeloghelper.handlers;

import javax.inject.Inject;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;

import de.anpross.eeloghelper.ParsingHelper;

public class UpdateAllLogStatementsInClassHandler extends AbstractHandler {
	@Inject
	IEclipseContext context;

	ParsingHelper parsingHelper;

	public UpdateAllLogStatementsInClassHandler() {
		parsingHelper = ContextInjectionFactory.make(ParsingHelper.class, context);
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// TODO Auto-generated method stub
		return null;
	}

}
