package de.anpross.eeloghelper.dtos;

import de.anpross.eeloghelper.enums.CallTypeAnnotationEnum;
import de.anpross.eeloghelper.enums.DefaultBehaviorEnum;

public class ClassDto implements AnnotatatedItem {

	private int bodyLineNumber;
	private int classLineNumber;
	private CallTypeAnnotationEnum callTypeAnnotation;
	private DefaultBehaviorEnum DefaultBehaviorEnum;

	@Override
	public int getBodyLineNumber() {
		return bodyLineNumber;
	}

	public void setBodyLineNumber(int bodyLineNumber) {
		this.bodyLineNumber = bodyLineNumber;
	}

	@Override
	public int getSignatureLineNumber() {
		return classLineNumber;
	}

	public void setSignatureLineNumber(int classLineNumber) {
		this.classLineNumber = classLineNumber;
	}

	public CallTypeAnnotationEnum getCallTypeAnnotation() {
		return callTypeAnnotation;
	}

	public void setCallTypeAnnotation(CallTypeAnnotationEnum annotation) {
		this.callTypeAnnotation = annotation;
	}

	public DefaultBehaviorEnum getDefaultBehaviorEnum() {
		return DefaultBehaviorEnum;
	}

	public void setDefaultBehaviorEnum(DefaultBehaviorEnum defaultBehaviorEnum) {
		DefaultBehaviorEnum = defaultBehaviorEnum;
	}

}
