package de.anpross.eeloghelper.visitors;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import de.anpross.eeloghelper.EeLogConstants;
import de.anpross.eeloghelper.StatementHelper;
import de.anpross.eeloghelper.dtos.ClassUpdateResultDto;
import de.anpross.eeloghelper.enums.LogStyleEnum;

/**
 * collects all method invocations to a specific Field. (the LOGGER)
 *
 * @author andreas
 *
 */
public class ClassUpdateVisitor extends ASTVisitor {

	private String fieldName;
	private List<ClassUpdateResultDto> methods;

	// because of anonymous inner classes, we can have a stack of methods
	// need to keep track of them because of return statements
	private Stack<MethodStackDto> currMethodStack;

	private class MethodStackDto {
		private String signature;
		private Expression returnExpression;
		private List<MethodInvocation> invocationsOfCurrMethod;
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

	}

	public ClassUpdateVisitor(String fieldName) {
		this.fieldName = fieldName;
		methods = new ArrayList<ClassUpdateResultDto>();
		currMethodStack = new Stack<MethodStackDto>();
	}

	@Override
	public boolean visit(MethodDeclaration node) {
		MethodStackDto method = new MethodStackDto();
		method.setSignature(StatementHelper.generateSignatureString(node));
		currMethodStack.push(method);
		return super.visit(node);
	}

	@Override
	public boolean visit(MethodInvocation node) {
		// method invocations can happen outside of methods,
		// however there should be no Log statements outside of Methods that need updating.
		// (declaring static blocks out-of-scope for now)
		if (!currMethodStack.isEmpty()) {
			Expression expression = node.getExpression();
			if (expression instanceof SimpleName) {
				SimpleName name = (SimpleName) expression;
				if (name.getIdentifier().equals(fieldName)) {
					currMethodStack.peek().getInvocationsOfCurrMethod().add(node);
				}
			}
		}
		return super.visit(node);
	}

	@Override
	public boolean visit(VariableDeclarationStatement node) {
		if (!currMethodStack.isEmpty()) {
			VariableDeclarationFragment firstStatement = StatementHelper.getFirstStatement(node);
			if (firstStatement.getName().getIdentifier().equals(EeLogConstants.CONST_NAME_LOG_METHOD)) {
				System.out.println(node);
				currMethodStack.peek().setLogStyle(LogStyleEnum.USE_VARIABLE);
			}
		}
		// TODO Auto-generated method stub
		return super.visit(node);
	}

	@Override
	public void endVisit(MethodDeclaration node) {
		MethodStackDto currMethod = currMethodStack.pop();
		for (MethodInvocation methodInvocation : currMethod.getInvocationsOfCurrMethod()) {
			addMethod(methodInvocation, currMethod);
		}
		super.endVisit(node);
	}

	@Override
	public boolean visit(ReturnStatement node) {
		currMethodStack.peek().setReturnExpression(node.getExpression());
		return super.visit(node);
	}

	private void addMethod(MethodInvocation invocation, MethodStackDto methodFromStack) {
		ClassUpdateResultDto result = new ClassUpdateResultDto();
		result.setInvocation(invocation);
		result.setSignature(methodFromStack.getSignature());
		result.setReturnExpression(methodFromStack.getReturnExpression());
		result.setLogStyle(methodFromStack.getLogStyle());
		methods.add(result);
	}

	public List<ClassUpdateResultDto> getMethods() {
		return methods;
	}
}
