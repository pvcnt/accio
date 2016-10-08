package fr.cnrs.liris.accio.cli;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation encoding metadata about an Accio command.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Command {
    /**
     * Name of this command, as the user would type it.
     */
    String name();

    /**
     * Flags processed by the command.
     */
    Class<?>[] flags() default {};

    /**
     * A short description, which appears in 'accio help'.
     */
    String help() default "";

    /**
     * The help message for this command. If the value starts with "resource:", the remainder is
     * interpreted as the name of a text file resource (in the .jar file that provides the command
     * implementation class).
     */
    String description() default "";

    /**
     * Specifies whether this command allows a residue after the parsed options.
     * For example, a command might expect a list of files to process in the residue.
     */
    boolean allowResidue() default false;

    /**
     * Specifies whether the command should not be shown in the output of 'accio help'.
     */
    boolean hidden() default false;
}