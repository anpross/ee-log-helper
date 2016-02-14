package de.anpross.eeloghelper;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Type;

public class EeLogConstants {
	public static final String CONST_NAME_LOG_CLASS = "LOG_CLASS";
	public static final String CONST_NAME_LOG_METHOD = "LOG_METHOD";
	public static final String CONST_NAME_DEFAULT_LEVEL = "DEFAULT_LEVEL";
	public static final String VARIABLE_NAME_ISLOGGING = "isLogging";
	public static final String VARIABLE_NAME_LOGGER = "LOGGER";
	public static final String PACKAGE_NAME_LANG = "java.lang";
	public static final String PACKAGE_NAME_LOGGING = "java.util.logging";
	public static final String CLASS_NAME_LOGGER = "Logger";
	public static final String CLASS_NAME_STRING = "String";
	public static final String CLASS_NAME_LEVEL = "Level";
	public static final String METHOD_NAME_ISLOGGABLE = "isLoggable";
	public static final String METHOD_NAME_GETLOGGER = "getLogger";

	public static QualifiedName getQNameLoggerType(AST ast) {
		return StatementHelper.getQName(EeLogConstants.PACKAGE_NAME_LOGGING, EeLogConstants.CLASS_NAME_LOGGER, ast);
	}

	public static QualifiedName getQNameLevelType(AST ast) {
		return StatementHelper.getQName(EeLogConstants.PACKAGE_NAME_LOGGING, EeLogConstants.CLASS_NAME_LEVEL, ast);
	}

	public static QualifiedName getQNameStringType(AST ast) {
		return StatementHelper.getQName(EeLogConstants.PACKAGE_NAME_LANG, EeLogConstants.CLASS_NAME_STRING, ast);
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

	public static SimpleName getNameLogger(AST ast) {
		return ast.newSimpleName(EeLogConstants.VARIABLE_NAME_LOGGER);
	}

	public static SimpleName getLoggerName(AST ast) {
		return ast.newSimpleName(EeLogConstants.VARIABLE_NAME_LOGGER);
	}

	public static SimpleName getLogClassName(AST ast) {
		return ast.newSimpleName(EeLogConstants.CONST_NAME_LOG_CLASS);
	}

	public static SimpleName getDefaultLevelName(AST ast) {
		return ast.newSimpleName(EeLogConstants.CONST_NAME_DEFAULT_LEVEL);
	}
}