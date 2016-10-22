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

package fr.cnrs.liris.accio.cli

import com.google.inject.{Inject, Singleton}
import fr.cnrs.liris.common.reflect.Annotations

case class CommandMeta(defn: Command, clazz: Class[_ <: AccioCommand])

@Singleton
class CommandRegistry @Inject()(classes: Set[Class[_ <: AccioCommand]]) {
  private[this] val index = classes.map { clazz =>
    val annotations = clazz.getAnnotations
    require(Annotations.exists[Command](annotations), "Commands must be annotated with @Command")
    val defn = Annotations.find[Command](annotations).get
    val meta = CommandMeta(defn, clazz)
    defn.name -> meta
  }.toMap

  def commands: Seq[CommandMeta] = index.values.toSeq.sortBy(_.defn.name)

  def get(name: String): Option[CommandMeta] = index.get(name)

  def contains(name: String): Boolean = index.contains(name)

  def apply(name: String): CommandMeta = index(name)
}