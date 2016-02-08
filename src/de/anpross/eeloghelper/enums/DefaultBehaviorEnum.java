package de.anpross.eeloghelper.enums;

public enum DefaultBehaviorEnum implements ItemAnnotation {
	DEFAULT_ON("default-on"),
	DEFAULT_OFF("default-off");

	String verb;

	DefaultBehaviorEnum(String verb) {
		this.verb = verb;
	}

	@Override
	public String getVerb() {
		return verb;
	}
}