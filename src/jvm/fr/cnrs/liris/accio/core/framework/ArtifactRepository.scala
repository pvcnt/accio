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

package fr.cnrs.liris.accio.core.framework

/**
 * A repository to store the results of node executions, so called artifacts. Note that despite its name, this
 * repository store [[Artifact]]s and metadata about node execution (completed [[NodeStatus]]).
 */
trait ArtifactRepository {
  /**
   * Save the result of a node execution under a given key.
   *
   * @param key    Node key.
   * @param status Completed node status.
   */
  def save(key: NodeKey, status: NodeStatus): Unit

  /**
   * Return the result of a node execution, if it exists.
   *
   * @param key Node key.
   */
  def get(key: NodeKey): Option[NodeStatus]

  /**
   * Check whether the result of a node execution is available.
   *
   * @param key Node key.
   */
  def exists(key: NodeKey): Boolean

  /**
   * Remove permanently the result of a node execution.
   *
   * @param key Node key.
   */
  def delete(key: NodeKey): Unit
}