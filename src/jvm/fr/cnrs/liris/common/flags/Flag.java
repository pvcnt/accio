package fr.cnrs.liris.common.flags;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An interface for annotating fields in classes (derived from OptionsBase)
 * that are options.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Flag {
    /**
     * The name of the option ("-name").
     */
    String name();

    /**
     * A help string for the usage information.
     */
    String help() default "";

    /**
     * A string describing the category of options that this belongs to. {@link
     * FlagsParser#describeFlags} prints options of the same category grouped
     * together.
     */
    String category() default "misc";

    /**
     * If the option is actually an abbreviation for other options, this field will
     * contain the strings to expand this option into. The original option is dropped
     * and the replacement used in its stead. It is recommended that such an option be
     * of type {@link Void}.
     * <p>
     * An expanded option overrides previously specified options of the same name,
     * even if it is explicitly specified. This is the original behavior and can
     * be surprising if the user is not aware of it, which has led to several
     * requests to change this behavior. This was discussed in the blaze team and
     * it was decided that it is not a strong enough case to change the behavior.
     */
    String[] expansion() default {};

    /**
     * If the option requires that additional options be implicitly appended, this field
     * will contain the additional options. Implicit dependencies are parsed at the end
     * of each {@link FlagsParser#parse} invocation, and override options specified in
     * the same call. However, they can be overridden by options specified in a later
     * call or by options with a higher priority.
     *
     * @see Priority
     */
    String[] implicitRequirements() default {};

    /**
     * If this field is a non-empty string, the option is deprecated, and a
     * deprecation warning is added to the list of warnings when such an option
     * is used.
     */
    String deprecationWarning() default "";
}