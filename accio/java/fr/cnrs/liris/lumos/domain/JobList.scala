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

package fr.cnrs.liris.lumos.domain

/**
 * Results and total number of jobs.
 *
 * @param results    List of jobs.
 * @param totalCount Total number of jobs.
 */
case class JobList(results: Seq[Job], totalCount: Long)

object JobList {
  def slice(items: Seq[Job], offset: Option[Int], limit: Option[Int]): JobList = {
    val totalCount = items.size
    var results = items
    offset.foreach { offset => results = results.drop(offset) }
    limit.foreach { limit => results = results.take(limit) }
    JobList(results, totalCount)
  }
}