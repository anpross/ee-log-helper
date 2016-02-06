package de.anpross.eeloghelper;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import de.anpross.eeloghelper.enums.MethodStateEnum;

/**
 * records all methods and class variables.
 *
 * @author andreas
 */
public class EntryExitLogVisitor extends ASTVisitor {

	MethodDeclaration currMethod;
	Statement firstStatement;
	Block currMethodBlock;

	List<FieldDeclaration> classFields = new ArrayList<FieldDeclaration>();
	List<MethodDto> methods = new ArrayList<MethodDto>();

	@Override
	public boolean visit(FieldDeclaration node) {
		if(currMethod == null) {
			classFields.add(node);
		}
		return super.visit(node);
	}

	@Override
	public void postVisit(ASTNode node) {
		super.postVisit(node);
	}

	@Override
	public void preVisit(ASTNode node) {
		if (node instanceof Statement && !(node instanceof Block) && firstStatement == null) {
			firstStatement = (Statement) node;
		}
		super.preVisit(node);
	}

	@Override
	public boolean visit(MethodDeclaration node) {
		currMethod = node;
		return super.visit(node);
	}

	@Override
	public void endVisit(MethodDeclaration node) {
		appendNewMethod();
		currMethod = null;
		currMethodBlock = null;
		firstStatement = null;
		super.endVisit(node);
	}

	private void appendNewMethod() {
		MethodDto newMethod = new MethodDto();
		newMethod.setMethodBlock(currMethodBlock);
		newMethod.setMethodDeclaration(currMethod);
		newMethod.setSignatureString(generateSignatureString(currMethod));
		newMethod.setMethodState(evaluateMethodState());

		methods.add(newMethod);
	}

	private MethodStateEnum evaluateMethodState() {
		if (firstStatement != null && StatementHelper.isStatementLoggingStatement(firstStatement)) {
			String correctSignature = generateSignatureString(currMethod);
			VariableDeclarationStatement variableDeclarationStmt = (VariableDeclarationStatement)firstStatement;
			if(StatementHelper.isLoggingStatementSignatureCorrect(variableDeclarationStmt, correctSignature)) {
				return MethodStateEnum.CORRECT;
			} else {
				return MethodStateEnum.WRONG_SIGNATURE;
			}
		} else {
			return MethodStateEnum.MISSING;
		}
	}

	private String generateSignatureString(MethodDeclaration currMethod) {
		boolean firstParameter = true;
		List<?> parameters = currMethod.parameters();
		StringBuilder signature = new StringBuilder();
		signature.append(currMethod.getName().getIdentifier());
		signature.append('(');

		for (Object object : parameters) {
			if(object instanceof SingleVariableDeclaration) {
				SingleVariableDeclaration currParameter = (SingleVariableDeclaration) object;
				if(!firstParameter) {
					signature.append(", ");
				}
				signature.append(currParameter.getType().toString());
				firstParameter = false;
			}
		}
		signature.append(')');
		return signature.toString();
	}

	@Override
	public boolean visit(Block node) {
		if (currMethodBlock == null) {
			currMethodBlock = node;
		}
		return super.visit(node);
	}

	public List<MethodDto> getMethods() {
		return methods;
	}

	public List<FieldDeclaration> getClassVariables() {
		return classFields;
	}

}
