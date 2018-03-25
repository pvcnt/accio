/*
 * Accio is a program whose purpose is to study location privacy.
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

package fr.cnrs.liris.accio.framework.scheduler.standalone

import com.google.inject.Inject
import com.typesafe.scalalogging.LazyLogging
import fr.cnrs.liris.accio.framework.api.thrift.{Resource, Task}

class TaskAssigner @Inject()(fitnessCalculator: FitnessCalculator) extends LazyLogging {
  def assign(task: Task, workers: Set[WorkerInfo]): Option[WorkerInfo] = {
    val matchingWorkers = enforceConstraints(task, workers)
    if (matchingWorkers.isEmpty) {
      if (!workers.exists(worker => isEnough(task.resource, worker.maxResources))) {
        logger.warn(s"No worker has enough resource to ever schedule task ${task.id.value} (op: ${task.payload.op}, resource: ${task.resource})")
      }
      None
    } else {
      Some(matchingWorkers.maxBy(worker => fitnessCalculator.compute(worker, task.resource)))
    }
  }

  private def enforceConstraints(task: Task, workers: Set[WorkerInfo]): Set[WorkerInfo] = {
    workers.filter(worker => isEnough(task.resource, worker.availableResources))
  }

  private def isEnough(request: Resource, available: Resource) = {
    (available.cpu == 0 || request.cpu <= available.cpu) &&
      (available.ramMb == 0 || request.ramMb <= available.ramMb) &&
      (available.diskMb == 0 || request.diskMb <= available.diskMb)
  }
}