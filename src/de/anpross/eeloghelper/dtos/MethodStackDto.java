package de.anpross.eeloghelper.dtos;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import de.anpross.eeloghelper.enums.LogStyleEnum;

public class MethodStackDto {
	private String signature;
	private List callParameters;
	private Expression returnExpression;
	private List<MethodInvocation> invocationsOfCurrMethod;
	private VariableDeclarationFragment logMethodVariableFragment;
	private MethodDeclaration methodDeclaration;
	private LogStyleEnum logStyle;

	public MethodStackDto() {
		invocationsOfCurrMethod = new ArrayList<MethodInvocation>();
		logStyle = LogStyleEnum.USE_LITERAL;
	}

	public String getSignature() {
		return signature;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}

	public List getCallParameters() {
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

	public List<MethodInvocation> getInvocationsOfCurrMethod() {
		return invocationsOfCurrMethod;
	}

	public LogStyleEnum getLogStyle() {
		return logStyle;
	}

	public void setLogStyle(LogStyleEnum logStyle) {
		this.logStyle = logStyle;
	}

	public VariableDeclarationFragment getLogMethodVariable() {
		return logMethodVariableFragment;
	}

	public void setLogMethodVariable(VariableDeclarationFragment logMethodVariable) {
		this.logMethodVariableFragment = logMethodVariable;
	}

	public void setMethodDeclaration(MethodDeclaration methodDeclaration) {
		this.methodDeclaration = methodDeclaration;
	}

	public MethodDeclaration getMethodDeclaration() {
		return methodDeclaration;
	}
}