package fr.cnrs.liris.accio.core.framework;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Op {
    String name() default "";

    String help() default "";

    String description() default "";

    String category() default "misc";

    boolean unstable() default false;

    boolean ephemeral() default false;
}