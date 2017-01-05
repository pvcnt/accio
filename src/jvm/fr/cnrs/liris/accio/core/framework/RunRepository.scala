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

trait RunRepository {
  def find(query: RunQuery): RunList

  def save(run: Run): Unit

  def delete(id: RunId): Unit

  def get(id: RunId): Option[Run]

  def exists(id: RunId): Boolean
}

case class RunQuery(
  workflow: Option[String] = None,
  cluster: Option[String] = None,
  owner: Option[User] = None,
  environment: Option[String] = None,
  name: Option[String] = None,
  limit: Option[Int] = None,
  offset: Option[Int] = None)

case class RunList(results: Seq[Run], totalCount: Int)