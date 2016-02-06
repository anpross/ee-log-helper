package de.anpross.eeloghelper;

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
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import de.anpross.eeloghelper.enums.EntryExitEnum;

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

	@SuppressWarnings("unchecked")
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

	public static VariableDeclarationStatement createIsLoggingStatement(AST ast) {
		VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
		fragment.setName(ast.newSimpleName(VARIABLE_NAME_ISLOGGING));

		MethodInvocation methodInvocation = ast.newMethodInvocation();
		methodInvocation.setExpression(ast.newSimpleName(VARIABLE_NAME_LOGGER));
		methodInvocation.setName(ast.newSimpleName(METHOD_NAME_ISLOGGABLE));
		List arguments = methodInvocation.arguments();
		arguments.add(ast.newSimpleName(CONST_NAME_DEFAULT_LEVEL));
		fragment.setInitializer(methodInvocation);

		VariableDeclarationStatement statement = ast.newVariableDeclarationStatement(fragment);
		statement.modifiers().add(createFinalModifier(ast));
		statement.setType(ast.newPrimitiveType(PrimitiveType.BOOLEAN));

		return statement;
	}

	public static IfStatement getEntryLoggingStatement(AST ast) {
		return getEntryExitLoggingStatement(ast, EntryExitEnum.ENTRY);
	}

	public static IfStatement getExitingLoggingStatement(AST ast) {
		return getEntryExitLoggingStatement(ast, EntryExitEnum.EXIT);
	}

	private static IfStatement getEntryExitLoggingStatement(AST ast, EntryExitEnum entryExit) {
		MethodInvocation invocation = ast.newMethodInvocation();
		invocation.setExpression(ast.newName(VARIABLE_NAME_LOGGER));
		invocation.setName(ast.newSimpleName(entryExit.getMethodName()));
		List arguments = invocation.arguments();
		arguments.add(ast.newSimpleName(CONST_NAME_LOG_CLASS));
		arguments.add(ast.newSimpleName(CONST_NAME_LOG_METHOD));
		ExpressionStatement expression = ast.newExpressionStatement(invocation);
		return getIfLoggingStatement(ast, expression);
	}

	private static IfStatement getIfLoggingStatement(AST ast, Statement thenStatement) {
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
	@SuppressWarnings("unchecked")
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

	public static boolean isLoggingStatementSignatureCorrect(VariableDeclarationStatement stmt,
			String correctSignature) {
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
		invocation.setExpression(
				ast.newQualifiedName(ast.newName(PACKAGE_NAME_LOGGER), ast.newSimpleName(CLASS_NAME_LOGGER)));
		invocation.setName(ast.newSimpleName("getLogger"));
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
		assert(firstFragment instanceof VariableDeclarationFragment);
		return (VariableDeclarationFragment) firstFragment;
	}

	public static VariableDeclarationFragment getFirstStatement(FieldDeclaration stmt) {
		List<?> fragments = stmt.fragments();
		Object firstFragment = fragments.get(0);
		assert(firstFragment instanceof VariableDeclarationFragment);
		return (VariableDeclarationFragment) firstFragment;
	}

//	QualifiedName qNameLogger = getQName(EeLogConstants.PACKAGE_NAME_LOGGER, EeLogConstants.CLASS_NAME_LOGGER, ast);

	public static QualifiedName getQName(String packageName, String className, AST ast) {
		return ast.newQualifiedName(ast.newName(packageName), ast.newSimpleName(className));
	}

}
