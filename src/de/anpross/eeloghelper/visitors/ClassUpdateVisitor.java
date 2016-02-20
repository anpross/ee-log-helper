package de.anpross.eeloghelper.visitors;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import de.anpross.eeloghelper.dtos.LogMethodCallUpdateDto;
import de.anpross.eeloghelper.dtos.MethodStackDto;

/**
 * collects all method invocations to a specific Field. (the LOGGER)
 *
 * @author andreas
 *
 */
public class ClassUpdateVisitor extends ASTVisitor {

	private String fieldName;
	private List<LogMethodCallUpdateDto> methods;
	private Map<VariableDeclarationFragment, MethodDeclaration> logMethodVariableMap = new LinkedHashMap<VariableDeclarationFragment, MethodDeclaration>();

	// because of anonymous inner classes, we can have a stack of methods
	// need to keep track of them because of return statements
	private Stack<MethodStackDto> currMethodStack;

	public ClassUpdateVisitor(String fieldName) {
		this.fieldName = fieldName;
		methods = new ArrayList<LogMethodCallUpdateDto>();
		currMethodStack = new Stack<MethodStackDto>();
	}

	@Override
	public boolean visit(MethodDeclaration node) {
		MethodStackDto method = new MethodStackDto();
		method.setSignature(StatementHelper.generateSignatureString(node));
		method.setCallParameters(node.parameters());
		method.setMethodDeclaration(node);
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
			if (firstStatement.getName().getIdentifier().equals(EeLogConstants.getLogMethod())) {
				// currMethodStack.peek().setLogStyle(LogStyleEnum.USE_VARIABLE);
				currMethodStack.peek().setLogMethodVariable(firstStatement);
			}
		}
		return super.visit(node);
	}

	@Override
	public void endVisit(MethodDeclaration node) {
		MethodStackDto currMethod = currMethodStack.pop();
		for (MethodInvocation methodInvocation : currMethod.getInvocationsOfCurrMethod()) {
			addMethod(methodInvocation, currMethod);
		}
		if (currMethod.getLogMethodVariable() != null) {
			logMethodVariableMap.put(currMethod.getLogMethodVariable(), currMethod.getMethodDeclaration());
		}
		super.endVisit(node);
	}

	@Override
	public boolean visit(ReturnStatement node) {
		currMethodStack.peek().setReturnExpression(node.getExpression());
		return super.visit(node);
	}

	private void addMethod(MethodInvocation invocation, MethodStackDto methodFromStack) {
		LogMethodCallUpdateDto result = new LogMethodCallUpdateDto();
		result.setInvocation(invocation);
		result.setSignature(methodFromStack.getSignature());
		result.setCallParameters(methodFromStack.getCallParameters());
		result.setReturnExpression(methodFromStack.getReturnExpression());
		result.setLogStyle(methodFromStack.getLogStyle());
		methods.add(result);
	}

	public List<LogMethodCallUpdateDto> getMethods() {
		return methods;
	}

	public Map<VariableDeclarationFragment, MethodDeclaration> getLogMethodVariables() {
		return logMethodVariableMap;
	}
}
