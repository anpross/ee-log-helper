package de.anpross.eeloghelper;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.anpross.eeloghelper.enums.LogStyleEnum;

public class PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public static final String PREF_LOG_STYLE = "logStyle";
	public static final String PREF_LOG_CLASS_VAR_NAME = "logClassVarName";
	public static final String PREF_LOG_CLASS_VAR_DEFAULT = "LOG_CLASS";
	public static final String PREF_LOG_METHOD_VAR_NAME = "methodVarName";
	public static final String PREF_LOG_METHOD_VAR_DEFAULT = "logMethod";
	public static final String PREF_IS_LOGGING_VAR_NAME = "isLoggingVarName";
	public static final String PREF_IS_LOGGING_VAR_DEFAULT = "isLogging";
	public static final String PREF_LOGGER_VAR_NAME = "loggerVarName";
	public static final String PREF_LOGGER_VAR_DEFAULT = "LOGGER";
	public static final String PREF_DEFAULT_LEVEL_VAR_NAME = "defaultLevelVarName";
	public static final String PREF_DEFAULT_LEVEL_VAR_DEFAULT = "DEFAULT_LEVEL";

	public PreferencePage() {
		super(FieldEditorPreferencePage.GRID);
	}

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("configuration of the entry/exit logger helper plugin");
	}

	@Override
	protected void createFieldEditors() {
		String[][] logStyleOptions = { { "method name as String literal", LogStyleEnum.USE_LITERAL.name() },
				{ "method name as Constant", LogStyleEnum.USE_VARIABLE.name() } };
		RadioGroupFieldEditor logStyle = new RadioGroupFieldEditor(PREF_LOG_STYLE, "select a log style to use:", 1, logStyleOptions,
				getFieldEditorParent());
		addField(logStyle);

		StringFieldEditor logMethodVarName = new StringFieldEditor(PREF_LOG_METHOD_VAR_NAME, "variable to store log method", 20,
				getFieldEditorParent());
		addField(logMethodVarName);

		StringFieldEditor isLoggingVarName = new StringFieldEditor(PREF_IS_LOGGING_VAR_NAME, "variable to store if logging is enabled", 20,
				getFieldEditorParent());
		addField(isLoggingVarName);

		StringFieldEditor loggerConstName = new StringFieldEditor(PREF_LOGGER_VAR_NAME, "constant to store the Logger", 20,
				getFieldEditorParent());
		addField(loggerConstName);

		StringFieldEditor defaultLevelConstName = new StringFieldEditor(PREF_DEFAULT_LEVEL_VAR_NAME, "variable to store default log level",
				20, getFieldEditorParent());
		addField(defaultLevelConstName);

		StringFieldEditor logClassConstName = new StringFieldEditor(PREF_LOGGER_VAR_NAME, "constant to store the Logger ", 20,
				getFieldEditorParent());
		addField(logClassConstName);
	}

	@Override
	protected void performDefaults() {
		super.performDefaults();
	}

}
