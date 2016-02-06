package de.anpross.eeloghelper.enums;

public enum EntryExitEnum {
	ENTRY("entering"),
	EXIT("exiting");

	private String methodName;
	EntryExitEnum(String methodName) {
		this.methodName = methodName;
	}

	public String getMethodName() {
		return methodName;
	}
}
