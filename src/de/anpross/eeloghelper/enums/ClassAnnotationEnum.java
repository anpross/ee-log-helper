package de.anpross.eeloghelper.enums;

public enum ClassAnnotationEnum {
	/** isLogging will be checked inside every method */
	PER_CALL_EVAL("per-call"),

	/** isLogging will be checked only at Object creation */
	PER_INSTANCE_EVAL("per-instance");

	/** the string that is matched against the code */
	String verb;

	ClassAnnotationEnum(String mode) {
		this.verb = mode;
	}

	String getVerb() {
		return verb;
	}
}
