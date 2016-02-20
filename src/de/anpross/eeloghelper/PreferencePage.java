package de.anpross.eeloghelper;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.anpross.eeloghelper.enums.LogStyleEnum;

public class PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private static final String DESCRIPTION = "configuration of the entry/exit logger helper plugin";
	private static final String VARIABLE_TO_STORE_THE_LOG_CLASS = "variable to store the log-class";
	private static final String VARIABLE_TO_STORE_DEFAULT_LOG_LEVEL = "variable to store default log level";
	private static final String VARIABLE_TO_STORE_THE_LOGGER = "variable to store the Logger";
	private static final String VARIABLE_TO_STORE_IF_LOGGING_IS_ENABLED = "variable to store if logging is enabled";
	private static final String VARIABLE_TO_STORE_LOG_METHOD = "variable to store log method";
	private static final String SELECT_A_LOG_STYLE_TO_USE = "select a log style to use:";
	private static final String METHOD_NAME_AS_STRING_LITERAL = "method name as String literal";
	private static final String METHOD_NAME_AS_VARIABLE = "method name as variable";

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
		setDescription(DESCRIPTION);
	}

	@Override
	protected void createFieldEditors() {
		String[][] logStyleOptions = { { METHOD_NAME_AS_STRING_LITERAL, LogStyleEnum.USE_LITERAL.name() },
				{ METHOD_NAME_AS_VARIABLE, LogStyleEnum.USE_VARIABLE.name() } };
		RadioGroupFieldEditor logStyle = new RadioGroupFieldEditor(PREF_LOG_STYLE, SELECT_A_LOG_STYLE_TO_USE, 1, logStyleOptions,
				getFieldEditorParent());
		addField(logStyle);

		StringFieldEditor logMethodVarName = new StringFieldEditor(PREF_LOG_METHOD_VAR_NAME, VARIABLE_TO_STORE_LOG_METHOD,
				getFieldEditorParent());
		addField(logMethodVarName);

		StringFieldEditor isLoggingVarName = new StringFieldEditor(PREF_IS_LOGGING_VAR_NAME, VARIABLE_TO_STORE_IF_LOGGING_IS_ENABLED,
				getFieldEditorParent());
		addField(isLoggingVarName);

		StringFieldEditor loggerConstName = new StringFieldEditor(PREF_LOGGER_VAR_NAME, VARIABLE_TO_STORE_THE_LOGGER,
				getFieldEditorParent());
		addField(loggerConstName);

		StringFieldEditor defaultLevelConstName = new StringFieldEditor(PREF_DEFAULT_LEVEL_VAR_NAME, VARIABLE_TO_STORE_DEFAULT_LOG_LEVEL,
				getFieldEditorParent());
		addField(defaultLevelConstName);

		StringFieldEditor logClassConstName = new StringFieldEditor(PREF_LOGGER_VAR_NAME, VARIABLE_TO_STORE_THE_LOG_CLASS,
				getFieldEditorParent());
		addField(logClassConstName);
	}

	@Override
	protected void performDefaults() {
		super.performDefaults();
	}

}
