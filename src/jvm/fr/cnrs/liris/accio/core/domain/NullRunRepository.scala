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

package fr.cnrs.liris.accio.core.domain

/**
 * A run repository doing nothing.
 */
class NullRunRepository extends MutableRunRepository {
  override def save(run: Run): Unit = {}

  override def save(logs: Seq[RunLog]): Unit = {}

  override def remove(id: RunId): Unit = {}

  override def find(query: RunQuery): RunList = RunList(Seq.empty, 0)

  override def find(query: LogsQuery): Seq[RunLog] = Seq.empty

  override def get(id: RunId): Option[Run] = None

  override def get(cacheKey: CacheKey): Option[OpResult] = None

  override def contains(id: RunId): Boolean = false
}

/**
 * Singleton of [[NullRunRepository]].
 */
object NullRunRepository extends NullRunRepository
