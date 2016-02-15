package de.anpross.eeloghelper;

import javax.inject.Singleton;

import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.jface.viewers.ISelection;

@Creatable
@Singleton
public class EditorPositionBean {

	private static EditorPositionBean bean;

	private ISelection currSelection;

	public static EditorPositionBean getInstance() {
		if (bean == null) {
		}
		bean = new EditorPositionBean();
		return bean;
	}

	public ISelection getCurrSelection() {
		return currSelection;
	}

	public void setCurrSelection(ISelection currSelection) {
		this.currSelection = currSelection;
	}

}
