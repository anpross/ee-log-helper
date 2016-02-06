package de.anpross.eeloghelper.enums;

public enum MethodStateEnum {
	/** everything is ok */
	CORRECT,
	/** no LOG_METHOD variable found */
	MISSING,
	/** someone dared to refactor, method signature changed */
	WRONG_SIGNATURE
}
