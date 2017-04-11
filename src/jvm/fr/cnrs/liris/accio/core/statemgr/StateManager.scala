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

package fr.cnrs.liris.accio.core.statemgr

import com.twitter.scrooge.ThriftStruct
import fr.cnrs.liris.accio.core.api.{ObjectReference, User}
import fr.cnrs.liris.dal.core.api.Value

/**
 */
trait StateManager {
  /*def saveObject(ref: ObjectReference, obj: ThriftStruct): Unit

  def deleteObject(ref: ObjectReference): Unit

  def getObject[T <: ThriftStruct](ref: ObjectReference): Option[T]

  def listObjects[T <: ThriftStruct](query: ListQuery): ObjectList[T]*/

  def close(): Unit = {}
}

/*case class ListQuery(
  fields: Map[String, Set[Value]] = Map.empty,
  tags: Set[String] = Set.empty,
  owner: Option[User] = None,
  limit: Option[Int] = None,
  offset: Int = 0)

case class ObjectList[T](results: Seq[T], totalCount: Int)*/