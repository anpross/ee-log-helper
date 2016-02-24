package de.anpross.eeloghelper;

import javax.inject.Singleton;

import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.jface.viewers.ISelection;

@Creatable
@Singleton
public class EditorPositionBean {

	private ISelection currSelection;

	public ISelection getCurrSelection() {
		return currSelection;
	}

	public void setCurrSelection(ISelection currSelection) {
		this.currSelection = currSelection;
	}
}