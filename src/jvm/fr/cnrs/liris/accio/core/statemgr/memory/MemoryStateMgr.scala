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

package fr.cnrs.liris.accio.core.statemgr.memory

import com.google.inject.Singleton
import fr.cnrs.liris.accio.core.statemgr.StateManager

/**
 * State manager designed for a single-node deployment.
 */
@Singleton
final class MemoryStateMgr extends StateManager {
  /*private[this] val objects = new ConcurrentHashMap[ObjectType, mutable.Map[String, ThriftStruct]]().asScala

  override def saveObject(ref: ObjectReference, obj: ThriftStruct): Unit = {
    objects
      .getOrElseUpdate(ref.kind, new ConcurrentHashMap[String, ThriftStruct]().asScala)
      .update(ref.name.value, obj)
  }

  override def deleteObject(ref: ObjectReference): Unit = {
    objects.get(ref.kind).foreach(_.remove(ref.name.value))
  }

  override def getObject[T <: ThriftStruct](ref: ObjectReference): Option[T] = {
    objects.get(ref.kind).flatMap(_.get(ref.name.value)).map(_.asInstanceOf[T])
  }

  override def listObjects[T <: ThriftStruct](query: ListQuery): ObjectList[T] = {

  }*/
}