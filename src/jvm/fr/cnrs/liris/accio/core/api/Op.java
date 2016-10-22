/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016 Vincent Primault <vincent.primault@liris.cnrs.fr>
 *
 * Accio is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Accio is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Accio.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.cnrs.liris.accio.core.api;

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
     * Name of this operator. By default, it is the simple class name, with the "Op" suffix stripped.
     */
    String name() default "";

    /**
     * A short description.
     */
    String help() default "";

    /**
     * A help message for this operator. If the value starts with "resource:", the remainder is
     * interpreted as the name of a text file resource (in the .jar file that provides the operator
     * implementation class).
     */
    String description() default "";

    /**
     * A category, which is used to classify operators in 'accio help list-ops'.
     */
    String category() default "misc";

    /**
     * A deprecation message that will be used each time this operator is used in a workflow.
     */
    String deprecation() default "";
}