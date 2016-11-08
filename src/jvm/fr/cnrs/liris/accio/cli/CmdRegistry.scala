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

import java.util.NoSuchElementException

import com.google.inject.{Inject, Singleton}
import fr.cnrs.liris.common.reflect.Annotations

/**
 * Metadata about a command, i.e., its definition plus runtime information.
 *
 * @param defn     Command definition.
 * @param cmdClass Command class.
 */
case class CommandMeta(defn: Cmd, cmdClass: Class[_ <: Command])

/**
 * Stores all commands known to the Accio CLI application. It is immutable, all commands should be registered when
 * the object is created.
 *
 * @param classes Classes containing command implementations.
 */
@Singleton
class CmdRegistry @Inject()(classes: Set[Class[_ <: Command]]) {
  private[this] val index = classes.map { clazz =>
    val annotations = clazz.getAnnotations
    require(Annotations.exists[Cmd](annotations), "Commands must be annotated with @Cmd")
    val defn = Annotations.find[Cmd](annotations).get
    val meta = CommandMeta(defn, clazz)
    defn.name -> meta
  }.toMap

  /**
   * Return all commands known to this registry.
   */
  def commands: Set[CommandMeta] = index.values.toSet

  /**
   * Check whether the registry contains a command with given name.
   *
   * @param name Command name.
   */
  def contains(name: String): Boolean = index.contains(name)

  /**
   * Return command metadata for the given command name, if it exists.
   *
   * @param name Command name.
   */
  def get(name: String): Option[CommandMeta] = index.get(name)

  /**
   * Return command metadata for the given command name.
   *
   * @param name Command name.
   * @throws NoSuchElementException If there is no command with the given name.
   */
  @throws[NoSuchElementException]
  def apply(name: String): CommandMeta = index(name)
}