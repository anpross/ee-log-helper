package de.anpross.eeloghelper.enums;

public enum CallTypeAnnotationEnum implements ItemAnnotation {
	/** aka nothing */
	NONE(""),

	/** isLogging will be checked inside every method */
	PER_CALL_EVAL("per-call"),

	/** isLogging will be checked only at Object creation */
	PER_INSTANCE_EVAL("per-instance");

	/** the string that is matched against the code */
	String verb;

	CallTypeAnnotationEnum(String mode) {
		this.verb = mode;
	}

	@Override
	public String getVerb() {
		return verb;
	}
}
