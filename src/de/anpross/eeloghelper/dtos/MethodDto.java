package de.anpross.eeloghelper.dtos;

import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import de.anpross.eeloghelper.enums.MethodAnnotationEnum;
import de.anpross.eeloghelper.enums.MethodStateEnum;

public class MethodDto {

	String signatureString;
	MethodDeclaration methodDeclaration;
	Block methodBlock;
	MethodStateEnum methodState;
	MethodAnnotationEnum methodAnnontation;
	private int methodLineNumber;

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

	public MethodAnnotationEnum getAnnontation() {
		return methodAnnontation;
	}

	public void setAnnontation(MethodAnnotationEnum methodAnnontation) {
		this.methodAnnontation = methodAnnontation;
	}

	public int getMethodLineNumber() {
		return methodLineNumber;
	}

	public void setMethodLineNumber(int methodLineNumer) {
		this.methodLineNumber = methodLineNumer;
	}
}
