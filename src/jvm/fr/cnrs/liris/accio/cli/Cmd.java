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
public @interface Cmd {
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
     * A help message for this command. If the value starts with "resource:", the remainder is
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