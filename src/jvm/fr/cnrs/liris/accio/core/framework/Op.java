package fr.cnrs.liris.accio.core.framework;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation encoding metadata about an operator.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Op {
    /**
     * Name of this operator. By default, it is the simple class name, with "Op" suffix stripped.
     */
    String name() default "";

    /**
     * A short description, which appears in 'accio ops'.
     */
    String help() default "";

    /**
     * The help message for this operator. If the value starts with "resource:", the remainder is
     * interpreted as the name of a text file resource (in the .jar file that provides the operator
     * implementation class).
     */
    String description() default "";

    /**
     * A category, which is used to classify operators in 'accio ops'.
     */
    String category() default "misc";

    /**
     * Specifies whether the output of this operator is deterministic.
     */
    boolean unstable() default false;

    /**
     * Metrics this operator produces (in case of an evaluator or analyzer).
     */
    String[] metrics() default {};
}