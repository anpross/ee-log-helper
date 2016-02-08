package de.anpross.eeloghelper.enums;

/**
 * "annotation" are no real java annotations but annotating comments in the line directly above the method declaration smiliar to the way
 * tools like PMD or checkstyle use it. Disabling logging for a specific method would look like this:
 *
 * <pre>
 * // EELOG off
 * </pre>
 *
 * where EELOG is the prefix, and off is the annotation matching to the enum name of this enum.
 *
 * @author andreas
 */
public enum MethodAnnotationEnum implements ItemAnnotation {
	/** no annotation present */
	NONE(""),

	/** explicit no-log */
	OFF("off"),

	/** explicit do-log */
	ON("on");

	String verb;

	MethodAnnotationEnum(String verb) {
		this.verb = verb;
	}

	@Override
	public String getVerb() {
		return verb;
	}
}
