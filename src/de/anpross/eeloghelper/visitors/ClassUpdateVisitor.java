package de.anpross.eeloghelper.visitors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;

/**
 * collects all method invocations to a specific Field. (the LOGGER)
 *
 * @author andreas
 *
 */
public class ClassUpdateVisitor extends ASTVisitor {

	String fieldName;
	private List<MethodInvocation> methods;

	public ClassUpdateVisitor(String fieldName) {
		this.fieldName = fieldName;
		methods = new ArrayList<MethodInvocation>();
	}

	@Override
	public boolean visit(MethodInvocation node) {
		Expression expression = node.getExpression();
		if (expression instanceof SimpleName) {
			SimpleName name = (SimpleName) expression;
			if (name.getIdentifier().equals(fieldName)) {
				methods.add(node);
			}
		}
		return super.visit(node);
	}

	public List<MethodInvocation> getMethods() {
		return methods;
	}
}
