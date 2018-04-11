/*
 * Accio is a platform to launch computer science experiments.
 * Copyright (C) 2016-2018 Vincent Primault <v.primault@ucl.ac.uk>
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

package fr.cnrs.liris.accio.api

import fr.cnrs.liris.accio.api.thrift.NamedValue

/**
 * Various helpers.
 */
object Utils {
  /**
   * Generate a human-readable label for a list of parameters.
   *
   * @param params List of parameters.
   */
  def label(params: Seq[NamedValue]): String = {
    params.map { param =>
      var vStr = Values.stringify(param.value)
      if (vStr.contains('/')) {
        // Remove any slash that would be polluting directory name.
        vStr = vStr.substring(vStr.lastIndexOf('/') + 1)
      }
      s"${param.name}=$vStr"
    }.mkString(" ")
  }

  def toString(ref: thrift.Reference): String = s"${ref.step}/${ref.output}"

  def toString(user: thrift.User): String = s"${user.name}${user.email.map(email => s" <$email>").getOrElse("")}"

  def isActive(state: thrift.ExecState): Boolean =
    state match {
      case thrift.ExecState.Scheduled => true
      case thrift.ExecState.Running => true
      case _ => false
    }

  def isCompleted(state: thrift.ExecState): Boolean =
    state match {
      case thrift.ExecState.Successful => true
      case thrift.ExecState.Failed => true
      case thrift.ExecState.Killed => true
      case thrift.ExecState.Cancelled => true
      case _ => false
    }
}