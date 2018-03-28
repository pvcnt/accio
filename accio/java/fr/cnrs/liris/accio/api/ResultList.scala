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

/**
 * Results and total number of results.
 *
 * @param results    List of objects.
 * @param totalCount Total number of results.
 */
case class ResultList[T](results: Seq[T], totalCount: Int)

object ResultList {
  def slice[T](items: Seq[T], offset: Option[Int], limit: Option[Int]): ResultList[T] = {
    val totalCount = items.size
    var results = items
    offset.foreach { offset => results = results.drop(offset) }
    limit.foreach { limit => results = results.take(limit) }
    ResultList(results, totalCount)
  }
}