package de.anpross.eeloghelper.dtos;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;

public class ClassUpdateResultDto {
	private MethodInvocation invocation;
	private String signature;
	private Expression returnExpression;

	public MethodInvocation getInvocation() {
		return invocation;
	}

	public void setInvocation(MethodInvocation invocation) {
		this.invocation = invocation;
	}

	public String getSignature() {
		return signature;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}

	public Expression getReturnExpression() {
		return returnExpression;
	}

	public void setReturnExpression(Expression returnExpression) {
		this.returnExpression = returnExpression;
	}
}
