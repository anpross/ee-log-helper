package de.anpross.eeloghelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;

import de.anpross.eeloghelper.dtos.ClassUpdateResultDto;

public class LoggerMethodMacher {

	List<MethodMatcher> matchers;

	private final static String METHOD_ENTERING = "entering";
	private final static String METHOD_EXITING = "exiting";
	private final static String METHOD_LOGP = "logp";
	private final static String METHOD_LOGRB = "logrb";
	private final static String METHOD_THROWING = "throwing";
	private final static String PREFIX_ARRAY = "[L";
	private final static String TYPE_STRING = String.class.getCanonicalName();
	private final static String TYPE_LEVEL = Level.class.getCanonicalName();
	private final static String TYPE_OBJECT = Object.class.getCanonicalName();
	private final static String TYPE_OBJECT_ARRAY = PREFIX_ARRAY + TYPE_OBJECT;
	private final static String TYPE_THROWABLE = Throwable.class.getCanonicalName();

	public class MethodMatcher {

		private String methodName;
		private List<String> arguments;
		private Integer classParameterPos;
		private Integer methodParameterPos;
		private Integer potentialReturnParameterPos;
		private Integer potentialCallParameterPos;
		private MethodInvocation invocation;
		private List callParameters;
		private Expression returnExpression;

		public MethodMatcher(String methodName, List<String> arguments, Integer classParameterPos, Integer methodParameterPos,
				Integer potentialReturnParameterPos, Integer potentialCallParameterPos) {
			this.methodName = methodName;
			this.arguments = arguments;
			this.classParameterPos = classParameterPos;
			this.methodParameterPos = methodParameterPos;
			this.potentialReturnParameterPos = potentialReturnParameterPos;
			this.potentialCallParameterPos = potentialCallParameterPos;
		}

		public boolean match(MethodInvocation invocation) {
			if (!invocation.getName().getFullyQualifiedName().equals(methodName)) {
				return false;
			}

			List<Expression> realArguments = invocation.arguments();
			if (realArguments.size() != arguments.size()) {
				return false;
			}

			int currArgument = 0;
			for (Expression currRealArgument : realArguments) {
				String realArgumentTypeName = currRealArgument.resolveTypeBinding().getQualifiedName();
				String realArgumentTypeNameClass = getClassNameForBindingName(realArgumentTypeName);
				String matcherArgumentType = arguments.get(currArgument);
				try {
					if (!Class.forName(matcherArgumentType).isAssignableFrom(Class.forName(realArgumentTypeNameClass))) {
						return false;
					}
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
				currArgument++;
			}
			this.setInvocation(invocation);
			return true;
		}

		private String getClassNameForBindingName(String realArgumentTypeName) {
			if (realArgumentTypeName.endsWith("[]")) {
				return realArgumentTypeName.substring(0, realArgumentTypeName.length() - 2);
			} else {
				return realArgumentTypeName;
			}
		}

		public Integer getClassParameterPos() {
			return classParameterPos;
		}

		public Integer getMethodParameterPos() {
			return methodParameterPos;
		}

		public MethodInvocation getInvocation() {
			return invocation;
		}

		public void setInvocation(MethodInvocation invocation) {
			this.invocation = invocation;
		}

		public Integer getPotentialReturnParameterPos() {
			return potentialReturnParameterPos;
		}

		public void setReturnParameterPos(Integer returnParameterPos) {
			this.potentialReturnParameterPos = returnParameterPos;
		}

		public Expression getReturnExpression() {
			return returnExpression;
		}

		public void setReturnExpression(Expression returnExpression) {
			this.returnExpression = returnExpression;
		}

		public Integer getPotentialCallParameterPos() {
			return potentialCallParameterPos;
		}

		public void setPotentialCallParameterPos(Integer potentialCallParameterPos) {
			this.potentialCallParameterPos = potentialCallParameterPos;
		}

		public List getCallParameters() {
			return callParameters;
		}

		public void setCallParameters(List callParameters) {
			this.callParameters = callParameters;
		}

	}

	/**
	 * @see {@linkplain https://docs.oracle.com/javase/7/docs/api/java/util/logging/Logger.html}
	 */
	public LoggerMethodMacher() {
		matchers = new ArrayList<MethodMatcher>();
		// those CAN have input/return parameters, if they do, it will on the given position
		matchers.add(new MethodMatcher(METHOD_EXITING, Arrays.asList(TYPE_STRING, TYPE_STRING), 0, 1, 2, null));
		matchers.add(new MethodMatcher(METHOD_ENTERING, Arrays.asList(TYPE_STRING, TYPE_STRING), 0, 1, null, 2));

		matchers.add(new MethodMatcher(METHOD_ENTERING, Arrays.asList(TYPE_STRING, TYPE_STRING, TYPE_OBJECT), 0, 1, null, 2));
		matchers.add(new MethodMatcher(METHOD_ENTERING, Arrays.asList(TYPE_STRING, TYPE_STRING, TYPE_OBJECT_ARRAY), 0, 1, null, 2));
		matchers.add(new MethodMatcher(METHOD_EXITING, Arrays.asList(TYPE_STRING, TYPE_STRING, TYPE_OBJECT), 0, 1, 2, null));
		matchers.add(new MethodMatcher(METHOD_LOGP, Arrays.asList(TYPE_LEVEL, TYPE_STRING, TYPE_STRING), 1, 2, null, null));
		matchers.add(new MethodMatcher(METHOD_LOGP, Arrays.asList(TYPE_LEVEL, TYPE_STRING, TYPE_STRING, TYPE_OBJECT), 1, 2, null, null));
		matchers.add(
				new MethodMatcher(METHOD_LOGP, Arrays.asList(TYPE_LEVEL, TYPE_STRING, TYPE_STRING, TYPE_OBJECT_ARRAY), 1, 2, null, null));
		matchers.add(new MethodMatcher(METHOD_LOGP, Arrays.asList(TYPE_LEVEL, TYPE_STRING, TYPE_STRING, TYPE_THROWABLE), 1, 2, null, null));
		matchers.add(new MethodMatcher(METHOD_LOGRB, Arrays.asList(TYPE_LEVEL, TYPE_STRING, TYPE_STRING, TYPE_STRING), 1, 2, null, null));
		matchers.add(new MethodMatcher(METHOD_LOGRB, Arrays.asList(TYPE_LEVEL, TYPE_STRING, TYPE_STRING, TYPE_STRING, TYPE_OBJECT), 1, 2,
				null, null));
		matchers.add(new MethodMatcher(METHOD_LOGRB, Arrays.asList(TYPE_LEVEL, TYPE_STRING, TYPE_STRING, TYPE_STRING, TYPE_OBJECT_ARRAY), 1,
				2, null, null));
		matchers.add(new MethodMatcher(METHOD_LOGRB, Arrays.asList(TYPE_LEVEL, TYPE_STRING, TYPE_STRING, TYPE_STRING, TYPE_THROWABLE), 1, 2,
				null, null));
		matchers.add(new MethodMatcher(METHOD_THROWING, Arrays.asList(TYPE_STRING, TYPE_STRING, TYPE_THROWABLE), 0, 1, null, null));
	}

	public MethodMatcher getMethodMatcherIfMatching(ClassUpdateResultDto invocation) {
		for (Iterator<MethodMatcher> iterator = matchers.iterator(); iterator.hasNext();) {
			MethodMatcher methodMatcher = iterator.next();
			if (methodMatcher.match(invocation.getInvocation())) {
				methodMatcher.setCallParameters(invocation.getParameters());
				methodMatcher.setReturnExpression(invocation.getReturnExpression());
				invocation.setSignature(invocation.getSignature());
				return methodMatcher;
			}
		}
		return null;
	}
}
