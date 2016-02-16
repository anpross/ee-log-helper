package de.anpross.eeloghelper;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.anpross.eeloghelper.enums.LogStyleEnum;

public class PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("configuration of the entry/exit logger helper plugin");

	}

	@Override
	protected void createFieldEditors() {
		String[][] labelAndValues = { { "method name as String literal", LogStyleEnum.USE_LITERAL.name() },
				{ "method name as Constant", LogStyleEnum.USE_VARIABLE.name() } };
		RadioGroupFieldEditor logStyle = new RadioGroupFieldEditor("logStyle", "select a log style to use:", 1, labelAndValues,
				getFieldEditorParent());
		addField(logStyle);
	}

}
