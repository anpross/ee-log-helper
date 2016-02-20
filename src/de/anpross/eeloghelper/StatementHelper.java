package de.anpross.eeloghelper;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
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
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import de.anpross.eeloghelper.dtos.MethodDto;
import de.anpross.eeloghelper.enums.EntryExitEnum;
import de.anpross.eeloghelper.enums.LogStyleEnum;

@SuppressWarnings("unchecked")
public class StatementHelper {
	public static VariableDeclarationStatement createMethodNameStatement(MethodDto method, AST ast) {
		VariableDeclarationFragment newDeclarationFragment = createMethodNameFragment(method.getSignatureString(), ast);

		VariableDeclarationStatement newDeclaration = ast.newVariableDeclarationStatement(newDeclarationFragment);

		newDeclaration.modifiers().add(createFinalModifier(ast));
		newDeclaration.setType(ast.newSimpleType(ast.newSimpleName(EeLogConstants.CLASS_NAME_STRING)));

		return newDeclaration;
	}

	public static VariableDeclarationFragment createMethodNameFragment(MethodDeclaration declaration, AST ast) {
		return createMethodNameFragment(generateSignatureString(declaration), ast);
	}

	private static VariableDeclarationFragment createMethodNameFragment(String signatureString, AST ast) {
		VariableDeclarationFragment newDeclarationFragment = ast.newVariableDeclarationFragment();
		newDeclarationFragment.setName(EeLogConstants.getLogMethodName(ast));

		newDeclarationFragment.setInitializer(getStringLiteral(signatureString, ast));
		return newDeclarationFragment;
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
		fragment.setName(EeLogConstants.getIsLoggingName(ast));

		MethodInvocation methodInvocation = createIsLoggingMethodInvocation(ast);
		fragment.setInitializer(methodInvocation);
		return fragment;
	}

	public static MethodInvocation createIsLoggingMethodInvocation(AST ast) {
		MethodInvocation methodInvocation = ast.newMethodInvocation();
		methodInvocation.setExpression(EeLogConstants.getLoggerName(ast));
		methodInvocation.setName(ast.newSimpleName(EeLogConstants.METHOD_NAME_ISLOGGABLE));
		List<SimpleName> arguments = methodInvocation.arguments();
		arguments.add(EeLogConstants.getDefaultLevelName(ast));
		return methodInvocation;
	}

	public static IfStatement createEntryLoggingStatement(AST ast, Expression methodNameExpression) {
		List<Expression> arguments = createEntryLoggingArguments(ast, methodNameExpression);
		MethodInvocation invocation = createEntryLoggingInvocation(ast, arguments);
		ExpressionStatement expression = ast.newExpressionStatement(invocation);
		return createIfLoggingStatement(ast, expression);
	}

	public static MethodInvocation createEntryLoggingInvocation(AST ast, Expression callExpression, Expression methodNameExpression) {
		List<Expression> arguments = createEntryLoggingArguments(ast, methodNameExpression);
		arguments.add(callExpression);
		MethodInvocation invocation = createEntryLoggingInvocation(ast, arguments);
		return invocation;
	}

	public static MethodInvocation createEntryLoggingInvocation(AST ast, List<Expression> arguments) {
		MethodInvocation invocation = createEntryExitLoggingStatement(ast, EntryExitEnum.ENTRY, arguments);
		return invocation;
	}

	private static List<Expression> createEntryLoggingArguments(AST ast, Expression methodName) {
		List<Expression> arguments = new ArrayList<Expression>();
		arguments.add(EeLogConstants.getLogClassName(ast));
		arguments.add(methodName);
		return arguments;
	}

	public static IfStatement createEntryLoggingIfStatement(AST ast, Expression returnExpression, Expression methodNameExpression) {
		MethodInvocation invocation = createExitingLoggingInvocation(ast, returnExpression, methodNameExpression);
		ExpressionStatement expression = ast.newExpressionStatement(invocation);
		return createIfLoggingStatement(ast, expression);
	}

	public static IfStatement createExitingLoggingIfStatement(AST ast, Expression returnExpression, Expression methodNameExpression) {
		MethodInvocation invocation = createExitingLoggingInvocation(ast, returnExpression, methodNameExpression);
		ExpressionStatement expression = ast.newExpressionStatement(invocation);
		return createIfLoggingStatement(ast, expression);
	}

	public static MethodInvocation createExitingLoggingInvocation(AST ast, Expression returnExpression, Expression methodSignature) {
		List<Expression> loggingArguments = createExitingLoggingArguments(ast, returnExpression, methodSignature);
		MethodInvocation invocation = createEntryExitLoggingStatement(ast, EntryExitEnum.EXIT, loggingArguments);
		return invocation;
	}

	public static List<Expression> createExitingLoggingArguments(AST ast, Expression returnExpression, Expression methodSignature) {
		List<Expression> arguments = new ArrayList<Expression>();
		arguments.add(EeLogConstants.getLogClassName(ast));
		arguments.add(methodSignature);
		if (returnExpression != null) {
			arguments.add(returnExpression);
		}
		return arguments;
	}

	private static MethodInvocation createEntryExitLoggingStatement(AST ast, EntryExitEnum entryExit, List<Expression> arguments) {
		MethodInvocation invocation = ast.newMethodInvocation();
		invocation.setExpression(EeLogConstants.getLoggerName(ast));
		invocation.setName(ast.newSimpleName(entryExit.getMethodName()));

		List<Expression> invocationArguments = invocation.arguments();
		for (Expression currArg : arguments) {
			if (currArg != null) {
				// need to copy the node as one node can not have 2 parents
				Expression newNode = (Expression) ASTNode.copySubtree(ast, currArg);
				invocationArguments.add(newNode);
			}
		}

		return invocation;
	}

	private static IfStatement createIfLoggingStatement(AST ast, Statement thenStatement) {
		IfStatement statement = ast.newIfStatement();
		statement.setExpression(EeLogConstants.getIsLoggingName(ast));
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
			if (type.resolveBinding().getQualifiedName().equals(String.class.getCanonicalName())
					&& StatementHelper.hasFinalModifier(varDeclStatement.modifiers())
					&& StatementHelper.getVariableName(varDeclStatement).equals(EeLogConstants.getLogMethod())) {
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
		invocation.setExpression(EeLogConstants.getQNameLoggerType(ast));
		invocation.setName(ast.newSimpleName(EeLogConstants.METHOD_NAME_GETLOGGER));
		invocation.arguments().add(EeLogConstants.getLogClassName(ast));
		return invocation;
	}

	public static StringLiteral createClassNameStringLiteral(AbstractTypeDeclaration currClass, AST ast) {
		return getStringLiteral(currClass.getName().getIdentifier(), ast);
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

	public static Expression generateCallExpression(List parameters, AST ast) {
		ArrayList<SimpleName> parsedParameters = new ArrayList<SimpleName>();
		if (parameters != null) {
			for (Object currParameter : parameters) {
				if (currParameter instanceof SingleVariableDeclaration) {
					SingleVariableDeclaration currVar = (SingleVariableDeclaration) currParameter;
					parsedParameters.add(currVar.getName());
				}
			}
			if (parsedParameters.size() == 1) {
				return parsedParameters.get(0);
			} else if (parsedParameters.size() > 1) {
				ArrayCreation arrayCreation = ast.newArrayCreation();
				arrayCreation.setType(ast.newArrayType(ast.newSimpleType(ast.newSimpleName("Object"))));
				ArrayInitializer initializer = ast.newArrayInitializer();
				arrayCreation.setInitializer(initializer);
				List expressions = initializer.expressions();
				for (SimpleName currParameter : parsedParameters) {
					expressions.add(ast.newSimpleName(currParameter.getIdentifier()));
				}
				return arrayCreation;
			}
		}
		return null;
	}

	public static void insertStatementsToListRewrite(ListRewrite listRewrite, List<Statement> statements) {
		if (!statements.isEmpty()) {
			boolean isFirst = true;
			Statement previousStmt = null;
			for (Statement currStatement : statements) {
				if (isFirst) {
					listRewrite.insertFirst(currStatement, null);
				} else {
					listRewrite.insertAfter(currStatement, previousStmt, null);
				}
				isFirst = false;
				previousStmt = currStatement;
			}
		}
	}

	public static void insertExitLogStatement(AST ast, LogStyleEnum logStyle, MethodDto currMethod, ListRewrite listRewrite) {
		Expression methodNameExpression = getMethodNameExpression(ast, logStyle, currMethod);
		List<?> originalStatements = listRewrite.getOriginalList();
		if (lastStatementIsReturnStatement(originalStatements)) {
			ReturnStatement returnStatement = (ReturnStatement) originalStatements.get(originalStatements.size() - 1);
			Expression returnExpression = returnStatement.getExpression();
			IfStatement exitStmt = StatementHelper.createExitingLoggingIfStatement(ast, returnExpression, methodNameExpression);
			listRewrite.insertBefore(exitStmt, returnStatement, null);
		} else {
			IfStatement exitStmt = StatementHelper.createExitingLoggingIfStatement(ast, null, methodNameExpression);
			listRewrite.insertLast(exitStmt, null);
		}
	}

	public static void insertEntryLogStatement(AST ast, LogStyleEnum logStyle, MethodDto currMethod, List<Statement> statements) {
		Expression methodNameExpression = getMethodNameExpression(ast, logStyle, currMethod);
		IfStatement entryStmt = StatementHelper.createEntryLoggingStatement(ast, methodNameExpression);
		statements.add(entryStmt);
	}

	private static Expression getMethodNameExpression(AST ast, LogStyleEnum logStyle, MethodDto currMethod) {
		Expression methodNameExpression;
		if (LogStyleEnum.USE_LITERAL.equals(logStyle)) {
			methodNameExpression = StatementHelper.getStringLiteral(currMethod.getSignatureString(), ast);
		} else if (LogStyleEnum.USE_VARIABLE.equals(logStyle)) {
			methodNameExpression = EeLogConstants.getLogMethodName(ast);
		} else {
			throw new IllegalStateException("LogStyleEnum has invalid value");
		}
		return methodNameExpression;
	}

	private static boolean lastStatementIsReturnStatement(List<?> originalStatements) {
		if (originalStatements.size() >= 1) {
			Object lastStatement = originalStatements.get(originalStatements.size() - 1);
			if (lastStatement instanceof ReturnStatement) {
				return true;
			}
		}
		return false;
	}

	public static StringLiteral getStringLiteral(String value, AST ast) {
		StringLiteral newStringLiteral = ast.newStringLiteral();
		newStringLiteral.setLiteralValue(value);
		return newStringLiteral;
	}
}
