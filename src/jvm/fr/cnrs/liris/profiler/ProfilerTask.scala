/*
 * Copyright LIRIS-CNRS (2016)
 * Contributors: Vincent Primault <vincent.primault@liris.cnrs.fr>
 *
 * This software is a computer program whose purpose is to study location privacy.
 *
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software. You can use,
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty and the software's author, the holder of the
 * economic rights, and the successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean that it is complicated to manipulate, and that also
 * therefore means that it is reserved for developers and experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and, more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 */

package fr.cnrs.liris.profiler

import com.twitter.conversions.time._
import com.twitter.util.Duration

import scala.collection.mutable

/**
 * A type ypes of profiler task. Each type also defines description and
 * minimum duration in nanoseconds for it to be recorded as separate event and
 * not just be aggregated into the parent event.
 *
 * @param id
 * @param description           Human readable description for the task
 * @param minDuration           Threshold for skipping tasks in the profile in nanoseconds, unless --record_full_profiler_data
 *                              is used
 * @param color                 Default color of the task, when rendered in a chart
 * @param slowestInstancesCount How many of the slowest instances to keep. If 0, no slowest instance calculation is done
 */
class ProfilerTask private(val id: Int, val description: String, val minDuration: Duration, val color: Int, val slowestInstancesCount: Int) {
  /**
   * Whether the Profiler collects the slowest instances of this task.
   */
  def collectsSlowestInstances: Boolean = slowestInstancesCount > 0

  override def toString: String = description
}

object ProfilerTask {
  private[this] val registry = mutable.ListBuffer.empty[ProfilerTask]

  def apply(idx: Int): ProfilerTask = registry(idx)

  def apply(description: String, minDuration: Duration = Duration.Bottom, color: Int = 0x000000, slowestInstancesCount: Int = 0): ProfilerTask = {
    val id = registry.size
    val typ = new ProfilerTask(id, description, minDuration, color, slowestInstancesCount)
    registry += typ
    typ
  }

  def values: Seq[ProfilerTask] = registry.toSeq

  val Phase = ProfilerTask("phase marker", color = 0x336699)
  val Info = ProfilerTask("general information", color = 0x000066)
  val Spawn = ProfilerTask("local process spawn", color = 0x663366)
  val Exception = ProfilerTask("exception", color = 0xFFCC66)
  val Wait = ProfilerTask("thread wait", 5 milliseconds, 0x66CCCC)
  val CriticalPath = ProfilerTask("critical path", color = 0x666699)
  val CriticalPathComponent = ProfilerTask("critical path component", color = 0x666699)
  val Clustering = ProfilerTask("clustering")
  val Simulation = ProfilerTask("simulation")
  val SimulationCost = ProfilerTask("SA: cost")
  val SimulationNeighbor = ProfilerTask("SA: neighbor")
  val SimulationInitialSolution = ProfilerTask("SA: initial solution")
  val Evaluation = ProfilerTask("evaluation")
  val ProtectionMechanism = ProfilerTask("protection mechanism")
  val Analyze = ProfilerTask("analyze")
}