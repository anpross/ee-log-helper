package de.anpross.eeloghelper;

import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import de.anpross.eeloghelper.enums.MethodStateEnum;

public class MethodDto {

	String signatureString;
	MethodDeclaration methodDeclaration;
	Block methodBlock;
	MethodStateEnum methodState;
	
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

}
