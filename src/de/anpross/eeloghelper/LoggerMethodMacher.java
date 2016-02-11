package de.anpross.eeloghelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;

public class LoggerMethodMacher {

	List<MethodMatcher> matchers;

	private final static String METHOD_ENTERING = "entering";
	private final static String METHOD_EXITING = "exiting";
	private final static String METHOD_LOGP = "logp";
	private final static String METHOD_LOGRB = "logrb";
	private final static String METHOD_THROWING = "throwing";
	private final static String SUFFIX_ARRAY = "[]";
	private final static String TYPE_STRING = String.class.getCanonicalName();
	private final static String TYPE_OBJECT = Object.class.getCanonicalName();
	private final static String TYPE_OBJECT_ARRAY = TYPE_OBJECT + SUFFIX_ARRAY;
	private final static String TYPE_THROWABLE = Throwable.class.getCanonicalName();

	public class MethodMatcher {

		private String methodName;
		private List<String> arguments;
		private Integer classParameterPos;
		private Integer methodParameterPos;
		private MethodInvocation invocation;

		public MethodMatcher(String methodName, List<String> arguments, Integer classParameterPos, Integer methodParameterPos) {
			this.methodName = methodName;
			this.arguments = arguments;
			this.classParameterPos = classParameterPos;
			this.methodParameterPos = methodParameterPos;
		}

		public boolean match(MethodInvocation invocation) {
			if (!invocation.getName().getFullyQualifiedName().equals(methodName)) {
				return false;
			}

			List<SimpleName> realArguments = invocation.arguments();
			if (realArguments.size() != arguments.size()) {
				return false;
			}

			int currArgument = 0;
			for (SimpleName currRealArgument : realArguments) {
				String realArgumentTypeName = currRealArgument.resolveTypeBinding().getQualifiedName();
				String matcherArgumentType = arguments.get(currArgument);
				if (!matcherArgumentType.equals(realArgumentTypeName)) {
					return false;
				}
				currArgument++;
			}
			this.setInvocation(invocation);
			return true;
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

	}

	/**
	 * @see {@linkplain https://docs.oracle.com/javase/7/docs/api/java/util/logging/Logger.html}
	 */
	public LoggerMethodMacher() {
		matchers = new ArrayList<MethodMatcher>();
		matchers.add(new MethodMatcher(METHOD_ENTERING, Arrays.asList(TYPE_STRING, TYPE_STRING), 0, 1));
		matchers.add(new MethodMatcher(METHOD_ENTERING, Arrays.asList(TYPE_STRING, TYPE_STRING, TYPE_OBJECT), 0, 1));
		matchers.add(new MethodMatcher(METHOD_ENTERING, Arrays.asList(TYPE_STRING, TYPE_STRING, TYPE_OBJECT_ARRAY), 0, 1));
		matchers.add(new MethodMatcher(METHOD_EXITING, Arrays.asList(TYPE_STRING, TYPE_STRING), 0, 1));
		matchers.add(new MethodMatcher(METHOD_EXITING, Arrays.asList(TYPE_STRING, TYPE_STRING, TYPE_OBJECT), 0, 1));
		matchers.add(new MethodMatcher(METHOD_LOGP, Arrays.asList(TYPE_STRING, TYPE_STRING), 1, 2));
		matchers.add(new MethodMatcher(METHOD_LOGP, Arrays.asList(TYPE_STRING, TYPE_STRING, TYPE_OBJECT), 1, 2));
		matchers.add(new MethodMatcher(METHOD_LOGP, Arrays.asList(TYPE_STRING, TYPE_STRING, TYPE_OBJECT_ARRAY), 1, 2));
		matchers.add(new MethodMatcher(METHOD_LOGP, Arrays.asList(TYPE_STRING, TYPE_STRING, TYPE_THROWABLE), 1, 2));
		matchers.add(new MethodMatcher(METHOD_LOGRB, Arrays.asList(TYPE_STRING, TYPE_STRING, TYPE_STRING), 1, 2));
		matchers.add(new MethodMatcher(METHOD_LOGRB, Arrays.asList(TYPE_STRING, TYPE_STRING, TYPE_STRING, TYPE_OBJECT), 1, 2));
		matchers.add(new MethodMatcher(METHOD_LOGRB, Arrays.asList(TYPE_STRING, TYPE_STRING, TYPE_STRING, TYPE_OBJECT_ARRAY), 1, 2));
		matchers.add(new MethodMatcher(METHOD_LOGRB, Arrays.asList(TYPE_STRING, TYPE_STRING, TYPE_STRING, TYPE_THROWABLE), 1, 2));
		matchers.add(new MethodMatcher(METHOD_THROWING, Arrays.asList(TYPE_STRING, TYPE_STRING, TYPE_THROWABLE), 0, 1));
	}

	public MethodMatcher getMethodMatcherIfMatching(MethodInvocation invocation) {
		for (Iterator<MethodMatcher> iterator = matchers.iterator(); iterator.hasNext();) {
			MethodMatcher methodMatcher = iterator.next();
			if (methodMatcher.match(invocation)) {
				return methodMatcher;
			}
		}
		return null;
	}
}
