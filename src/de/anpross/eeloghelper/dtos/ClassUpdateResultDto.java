package de.anpross.eeloghelper.dtos;

import java.util.List;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;

import de.anpross.eeloghelper.enums.LogStyleEnum;

public class ClassUpdateResultDto {
	private MethodInvocation invocation;
	private String signature;
	private List callParameters;
	private Expression returnExpression;
	private LogStyleEnum logStyle;

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

	public List getParameters() {
		return callParameters;
	}

	public void setCallParameters(List callParameters) {
		this.callParameters = callParameters;
	}

	public Expression getReturnExpression() {
		return returnExpression;
	}

	public void setReturnExpression(Expression returnExpression) {
		this.returnExpression = returnExpression;
	}

	public LogStyleEnum getLogStyle() {
		return logStyle;
	}

	public void setLogStyle(LogStyleEnum logStyle) {
		this.logStyle = logStyle;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("ClassUpdateResultDto[");
		sb.append(signature).append(", ");
		sb.append(invocation).append("]");
		return sb.toString();
	}
}
