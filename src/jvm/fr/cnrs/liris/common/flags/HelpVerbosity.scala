/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016-2017 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.common.flags

/**
 * The verbosity with which option help messages are displayed.
 */
sealed class HelpVerbosity(protected val level: Int) extends Ordered[HelpVerbosity] {
  override def compare(that: HelpVerbosity): Int = level.compareTo(that.level)
}

object HelpVerbosity {

  /**
   * Display only the name of the flags.
   */
  case object Short extends HelpVerbosity(0)

  /**
   * Display the name, type and default value of the flags.
   */
  case object Medium extends HelpVerbosity(1)

  /**
   * Display the full description of the flags.
   */
  case object Long extends HelpVerbosity(2)

}
