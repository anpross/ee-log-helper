package de.anpross.eeloghelper;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import de.anpross.eeloghelper.dtos.MethodDto;
import de.anpross.eeloghelper.enums.EntryExitEnum;

@SuppressWarnings("unchecked")
public class StatementHelper {
	private static final String CONST_NAME_LOG_CLASS = "LOG_CLASS";
	private static final String CONST_NAME_LOG_METHOD = "LOG_METHOD";
	private static final String CONST_NAME_DEFAULT_LEVEL = "DEFAULT_LEVEL";
	private static final String VARIABLE_NAME_ISLOGGING = "isLogging";
	private static final String VARIABLE_NAME_LOGGER = "LOGGER";
	private static final String PACKAGE_NAME_LOGGER = "java.util.logging";
	private static final String CLASS_NAME_LOGGER = "Logger";
	private static final String CLASS_NAME_STRING = "String";
	private static final String METHOD_NAME_ISLOGGABLE = "isLoggable";

	public static VariableDeclarationStatement createMethodNameStatement(MethodDto method, AST ast) {
		VariableDeclarationFragment newDeclarationFragment = ast.newVariableDeclarationFragment();
		newDeclarationFragment.setName(ast.newSimpleName(CONST_NAME_LOG_METHOD));

		StringLiteral methodSignature = ast.newStringLiteral();
		methodSignature.setLiteralValue(method.getSignatureString());
		newDeclarationFragment.setInitializer(methodSignature);

		VariableDeclarationStatement newDeclaration = ast.newVariableDeclarationStatement(newDeclarationFragment);

		newDeclaration.modifiers().add(createFinalModifier(ast));
		newDeclaration.setType(ast.newSimpleType(ast.newSimpleName(CLASS_NAME_STRING)));

		return newDeclaration;
	}

	private static Modifier createFinalModifier(AST ast) {
		return ast.newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD);
	}

	public static VariableDeclarationExpression createIsLoggingExpresion(AST ast) {
		VariableDeclarationFragment fragment = createIsLoggingFragment(ast);
		VariableDeclarationExpression expression = ast.newVariableDeclarationExpression(fragment);
		return expression;
	}

	public static VariableDeclarationStatement createIsLoggingStatement(AST ast) {
		VariableDeclarationFragment fragment = createIsLoggingFragment(ast);

		VariableDeclarationStatement statement = ast.newVariableDeclarationStatement(fragment);
		statement.modifiers().add(createFinalModifier(ast));
		statement.setType(ast.newPrimitiveType(PrimitiveType.BOOLEAN));
		return statement;
	}

	public static VariableDeclarationFragment createIsLoggingFragment(AST ast) {
		VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
		fragment.setName(ast.newSimpleName(VARIABLE_NAME_ISLOGGING));

		MethodInvocation methodInvocation = createIsLoggingMethodInvocation(ast);
		fragment.setInitializer(methodInvocation);
		return fragment;
	}

	public static MethodInvocation createIsLoggingMethodInvocation(AST ast) {
		MethodInvocation methodInvocation = ast.newMethodInvocation();
		methodInvocation.setExpression(ast.newSimpleName(VARIABLE_NAME_LOGGER));
		methodInvocation.setName(ast.newSimpleName(METHOD_NAME_ISLOGGABLE));
		List<SimpleName> arguments = methodInvocation.arguments();
		arguments.add(ast.newSimpleName(CONST_NAME_DEFAULT_LEVEL));
		return methodInvocation;
	}

	public static IfStatement createEntryLoggingStatement(AST ast) {
		List<Expression> arguments = new ArrayList<Expression>();
		arguments.add(ast.newSimpleName(CONST_NAME_LOG_CLASS));
		arguments.add(ast.newSimpleName(CONST_NAME_LOG_METHOD));
		MethodInvocation invocation = createEntryExitLoggingStatement(ast, EntryExitEnum.ENTRY, arguments);
		ExpressionStatement expression = ast.newExpressionStatement(invocation);
		return createIfLoggingStatement(ast, expression);
	}

	public static IfStatement createEntryLoggingIfStatement(AST ast, Expression returnExpression) {
		MethodInvocation invocation = createExitingLoggingInvocation(ast, returnExpression);
		ExpressionStatement expression = ast.newExpressionStatement(invocation);
		return createIfLoggingStatement(ast, expression);
	}

	public static IfStatement createExitingLoggingIfStatement(AST ast, Expression returnExpression) {
		MethodInvocation invocation = createExitingLoggingInvocation(ast, returnExpression);
		ExpressionStatement expression = ast.newExpressionStatement(invocation);
		return createIfLoggingStatement(ast, expression);
	}

	public static MethodInvocation createExitingLoggingInvocation(AST ast, Expression returnExpression) {
		return createExitingLoggingInvocation(ast, returnExpression, ast.newSimpleName(CONST_NAME_LOG_METHOD));
	}

	public static MethodInvocation createExitingLoggingInvocation(AST ast, Expression returnExpression, Expression methodSignature) {
		List<Expression> loggingArguments = createExitingLoggingArguments(ast, returnExpression, methodSignature);
		MethodInvocation invocation = createEntryExitLoggingStatement(ast, EntryExitEnum.EXIT, loggingArguments);
		return invocation;
	}

	public static List<Expression> createExitingLoggingArguments(AST ast, Expression returnExpression, Expression methodSignature) {
		List<Expression> arguments = new ArrayList<Expression>();
		arguments.add(ast.newSimpleName(CONST_NAME_LOG_CLASS));
		arguments.add(methodSignature);
		if (returnExpression != null) {
			arguments.add(returnExpression);
		}
		return arguments;
	}

	private static MethodInvocation createEntryExitLoggingStatement(AST ast, EntryExitEnum entryExit, List<Expression> arguments) {
		MethodInvocation invocation = ast.newMethodInvocation();
		invocation.setExpression(ast.newName(VARIABLE_NAME_LOGGER));
		invocation.setName(ast.newSimpleName(entryExit.getMethodName()));

		List<Expression> invocationArguments = invocation.arguments();
		for (Expression currArg : arguments) {
			// need to copy the node as one node can not have 2 parents
			Expression newNode = (Expression) ASTNode.copySubtree(ast, currArg);
			invocationArguments.add(newNode);
		}

		return invocation;
	}

	private static IfStatement createIfLoggingStatement(AST ast, Statement thenStatement) {
		IfStatement statement = ast.newIfStatement();
		statement.setExpression(ast.newSimpleName(VARIABLE_NAME_ISLOGGING));
		Block thenBlock = ast.newBlock();
		thenBlock.statements().add(thenStatement);
		statement.setThenStatement(thenBlock);
		return statement;
	}

	/**
	 * checks if the statement is a: <br/>
	 * "final String LOG_METHOD"
	 *
	 * @param stmt
	 * @return
	 */
	public static boolean isStatementLoggingStatement(Statement stmt) {
		if (stmt.getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT) {
			VariableDeclarationStatement varDeclStatement = (VariableDeclarationStatement) stmt;
			Type type = varDeclStatement.getType();
			if (type.resolveBinding().getQualifiedName().equals("java.lang.String")
					&& StatementHelper.hasFinalModifier(varDeclStatement.modifiers())
					&& StatementHelper.getVariableName(varDeclStatement).equals(CONST_NAME_LOG_METHOD)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isLoggingStatementSignatureCorrect(VariableDeclarationStatement stmt, String correctSignature) {
		return correctSignature.equals(getVariableDelcarationStringValue(stmt));
	}

	private static String getVariableDelcarationStringValue(VariableDeclarationStatement stmt) {
		VariableDeclarationFragment firstFragment = (VariableDeclarationFragment) stmt.fragments().get(0);
		Expression initializer = firstFragment.getInitializer();
		if (initializer instanceof StringLiteral) {
			StringLiteral stringLiteral = (StringLiteral) initializer;
			return stringLiteral.getLiteralValue();
		}
		return null;
	}

	private static String getVariableName(VariableDeclarationStatement varDeclStatement) {
		VariableDeclarationFragment firstStatement = StatementHelper.getFirstStatement(varDeclStatement);
		return firstStatement.getName().getIdentifier();
	}

	private static boolean hasFinalModifier(List<IExtendedModifier> modifiers) {
		for (Object currModifer : modifiers) {
			if (currModifer instanceof Modifier) {
				if (((Modifier) currModifer).isFinal()) {
					return true;
				}
			}
		}
		return false;
	}

	public static MethodInvocation createLoggerStatement(AST ast) {
		MethodInvocation invocation = ast.newMethodInvocation();
		invocation.setExpression(ast.newQualifiedName(ast.newName(PACKAGE_NAME_LOGGER), ast.newSimpleName(CLASS_NAME_LOGGER)));
		invocation.setName(ast.newSimpleName(EeLogConstants.METHOD_NAME_GETLOGGER));
		invocation.arguments().add(ast.newSimpleName(CONST_NAME_LOG_CLASS));
		return invocation;
	}

	public static StringLiteral createClassNameStringLiteral(AbstractTypeDeclaration currClass, AST ast) {
		StringLiteral stringLiteral = ast.newStringLiteral();
		stringLiteral.setLiteralValue(currClass.getName().getIdentifier());
		return stringLiteral;
	}

	public static Expression createLogLevelStatement(Level finer, AST ast) {
		return ast.newQualifiedName(ast.newName("Level"), ast.newSimpleName(finer.getName()));
	}

	public static VariableDeclarationStatement getFirstVariableDeclarationStatementOfBlock(Block block) {
		for (Object currStatementObj : block.statements()) {
			if (currStatementObj instanceof VariableDeclarationStatement) {
				VariableDeclarationStatement firstVariableDeclarationStmt = (VariableDeclarationStatement) currStatementObj;
				return firstVariableDeclarationStmt;
			}
		}
		return null;
	}

	public static VariableDeclarationFragment getFirstStatement(VariableDeclarationStatement stmt) {
		List<?> fragments = stmt.fragments();
		Object firstFragment = fragments.get(0);
		assert (firstFragment instanceof VariableDeclarationFragment);
		return (VariableDeclarationFragment) firstFragment;
	}

	public static VariableDeclarationFragment getFirstStatement(FieldDeclaration stmt) {
		List<?> fragments = stmt.fragments();
		Object firstFragment = fragments.get(0);
		assert (firstFragment instanceof VariableDeclarationFragment);
		return (VariableDeclarationFragment) firstFragment;
	}

	public static QualifiedName getQName(String packageName, String className, AST ast) {
		return ast.newQualifiedName(ast.newName(packageName), ast.newSimpleName(className));
	}

	public static String generateSignatureString(MethodDeclaration currMethod) {
		boolean firstParameter = true;
		List<?> parameters = currMethod.parameters();
		StringBuilder signature = new StringBuilder();
		signature.append(currMethod.getName().getIdentifier());
		signature.append('(');

		for (Object object : parameters) {
			if (object instanceof SingleVariableDeclaration) {
				SingleVariableDeclaration currParameter = (SingleVariableDeclaration) object;
				if (!firstParameter) {
					signature.append(", ");
				}
				signature.append(currParameter.getType().toString());
				firstParameter = false;
			}
		}
		signature.append(')');
		return signature.toString();
	}
}
