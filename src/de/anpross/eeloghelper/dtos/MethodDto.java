package de.anpross.eeloghelper.dtos;

import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import de.anpross.eeloghelper.enums.MethodAnnotationEnum;
import de.anpross.eeloghelper.enums.MethodStateEnum;

public class MethodDto implements AnnotatatedItem {

	String signatureString;
	MethodDeclaration methodDeclaration;
	Block methodBlock;
	MethodStateEnum methodState;
	MethodAnnotationEnum methodAnnotation;
	private int methodLineNumber;
	private int bodyLineNumber;

	public String getSignatureString() {
		return signatureString;
	}

	public void setSignatureString(String signatureString) {
		this.signatureString = signatureString;
	}

	public MethodDeclaration getMethodDeclaration() {
		return methodDeclaration;
	}

	public void setMethodDeclaration(MethodDeclaration methodDeclaration) {
		this.methodDeclaration = methodDeclaration;
	}

	public Block getMethodBlock() {
		return methodBlock;
	}

	public void setMethodBlock(Block methodBlock) {
		this.methodBlock = methodBlock;
	}

	public MethodStateEnum getMethodState() {
		return methodState;
	}

	public void setMethodState(MethodStateEnum methodState) {
		this.methodState = methodState;
	}

	public MethodAnnotationEnum getAnnotation() {
		return methodAnnotation;
	}

	public void setAnnotation(MethodAnnotationEnum methodAnnontation) {
		this.methodAnnotation = methodAnnontation;
	}

	@Override
	public int getSignatureLineNumber() {
		// TODO Auto-generated method stub
		return methodLineNumber;
	}

	public void setSignatureLineNumber(int methodLineNumer) {
		this.methodLineNumber = methodLineNumer;
	}

	@Override
	public int getBodyLineNumber() {
		return bodyLineNumber;
	}

	public void setBodyLineNumber(int bodyLineNumber) {
		this.bodyLineNumber = bodyLineNumber;
	}
}
