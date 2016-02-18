package de.anpross.eeloghelper;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Type;

public class EeLogConstants {
	public static final String PACKAGE_NAME_LANG = "java.lang";
	public static final String PACKAGE_NAME_LOGGING = "java.util.logging";
	public static final String CLASS_NAME_LOGGER = "Logger";
	public static final String CLASS_NAME_STRING = "String";
	public static final String CLASS_NAME_LEVEL = "Level";
	public static final String CLASS_NAME_OBJECT = "Object";
	public static final String METHOD_NAME_GETLOGGER = "getLogger";
	public static final String METHOD_NAME_ISLOGGABLE = "isLoggable";

	public static QualifiedName getQNameLoggerType(AST ast) {
		return StatementHelper.getQName(EeLogConstants.PACKAGE_NAME_LOGGING, EeLogConstants.CLASS_NAME_LOGGER, ast);
	}

	public static QualifiedName getQNameLevelType(AST ast) {
		return StatementHelper.getQName(EeLogConstants.PACKAGE_NAME_LOGGING, EeLogConstants.CLASS_NAME_LEVEL, ast);
	}

	public static QualifiedName getQNameStringType(AST ast) {
		return StatementHelper.getQName(EeLogConstants.PACKAGE_NAME_LANG, EeLogConstants.CLASS_NAME_STRING, ast);
	}

	public static QualifiedName getQNameObjectType(AST ast) {
		return StatementHelper.getQName(EeLogConstants.PACKAGE_NAME_LANG, EeLogConstants.CLASS_NAME_OBJECT, ast);
	}

	public static Type getTypeString(AST ast) {
		return ast.newSimpleType(getQNameStringType(ast));
	}

	public static Type getLoggerType(AST ast) {
		return ast.newSimpleType(getQNameLoggerType(ast));
	}

	public static Type getTypeLevel(AST ast) {
		return ast.newSimpleType(getQNameLevelType(ast));
	}

	public static Type getTypeObject(AST ast) {
		return ast.newSimpleType(getQNameObjectType(ast));
	}

	public static SimpleName getLoggerName(AST ast) {
		return ast.newSimpleName(getLogger());
	}

	public static String getLogger() {
		return getPreference(PreferencePage.PREF_LOGGER_VAR_NAME);
	}

	public static SimpleName getLogClassName(AST ast) {
		return ast.newSimpleName(getPreference(PreferencePage.PREF_LOG_CLASS_VAR_NAME));
	}

	public static SimpleName getDefaultLevelName(AST ast) {
		return ast.newSimpleName(getPreference(PreferencePage.PREF_DEFAULT_LEVEL_VAR_NAME));
	}

	public static SimpleName getLogMethodName(AST ast) {
		return ast.newSimpleName(getLogMethod());
	}

	public static String getLogMethod() {
		return getPreference(PreferencePage.PREF_LOG_METHOD_VAR_NAME);
	}

	public static SimpleName getIsLoggingName(AST ast) {
		return ast.newSimpleName(getPreference(PreferencePage.PREF_IS_LOGGING_VAR_NAME));
	}

	private static String getPreference(String preference) {
		return Activator.getDefault().getPreferenceStore().getString(preference);
	}
}