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

package fr.cnrs.liris.accio.core.scheduler.standalone

import fr.cnrs.liris.accio.core.api.Resource
import fr.cnrs.liris.accio.core.scheduler.WorkerInfo

trait FitnessCalculator {
  def name: String

  def compute(worker: WorkerInfo, request: Resource): Double
}

abstract class BinPacker extends FitnessCalculator {
  final override def compute(worker: WorkerInfo, request: Resource): Double = {
    val total = (get(worker.reservedResources) + get(request)).round
    if (worker.maxResources.cpu > worker.maxResources.cpu) {
      0
    } else {
      val max = get(worker.maxResources)
      if (max > 0) total / get(worker.maxResources) else 0
    }
  }

  protected def get(resources: Resource): Double
}

class CpuBinPacker extends BinPacker {
  override def name: String = "cpuBinPacker"

  override protected def get(resources: Resource): Double = resources.cpu
}

class RamBinPacker extends BinPacker {
  override def name: String = "ramBinPacker"

  override protected def get(resources: Resource): Double = resources.ramMb
}

class DiskBinPacker extends BinPacker {
  override def name: String = "diskBinPacker"

  override protected def get(resources: Resource): Double = resources.diskMb
}

class ComposedFitnessCalculator(calculators: Seq[FitnessCalculator]) extends FitnessCalculator {
  require(calculators.nonEmpty, "You should specify at least one fitness calculator")

  override def name: String = {
    val names = calculators.map(_.name)
    (names.head ++ names.tail.map(_.capitalize)).mkString("And")
  }

  override def compute(worker: WorkerInfo, request: Resource): Double = {
    calculators.map(_.compute(worker, request)).sum / calculators.size
  }
}