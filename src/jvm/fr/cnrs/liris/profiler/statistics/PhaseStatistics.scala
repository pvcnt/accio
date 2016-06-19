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

package fr.cnrs.liris.profiler.statistics

import com.twitter.util.Duration
import fr.cnrs.liris.profiler.{AggregateAttr, ProfileInfo, ProfilePhase, ProfilerTask}
import fr.cnrs.liris.common.util.Requirements._

/**
 * Extracts and keeps statistics for one [[ProfilePhase]] for formatting to various outputs.
 */
class PhaseStatistics(val phase: ProfilePhase, info: ProfileInfo) extends Iterable[ProfilerTask] {
  private val phaseTask = info.phaseTask(phase)
  private val taskStats = phaseTask.map { phaseTask =>
    val tasks = info.tasksForPhase(phaseTask)
    ProfilerTask.values.map { typ =>
      typ -> info.statsForType(typ, tasks)
    }.toMap
  }.getOrElse(Map.empty[ProfilerTask, AggregateAttr])

  /**
   * Return the duration of the phase.
   */
  val phaseDuration = phaseTask.map(info.phaseDuration).getOrElse(Duration.Zero)

  /**
   * Return the total duration of the phase.
   */
  val totalDuration = phaseDuration + phaseTask.map { phaseTask =>
    info.tasksForPhase(phaseTask)
        // Tasks on the phaseTask thread already accounted for in the phaseDuration.
        .filter(_.threadId != phaseTask.threadId)
        .map(_.duration)
        .foldLeft(Duration.Zero)(_ + _)
  }.getOrElse(Duration.Zero)

  /**
   * @return how many executions of this phase were accumulated
   */
  def phaseCount: Int = 1

  /**
   * Add statistics accumulated in another PhaseStatistics object to this one.
   */
  /*def add(other: PhaseStatistics): Unit = {
    require(phase == other.phase, "Should not combine statistics from different phases")
    if (other._wasExecuted) {
      _wasExecuted = true
      _phaseDuration += other._phaseDuration
      _totalDuration += other._totalDuration
      other.foreach { typ =>
        val otherCount = other.getCount(typ)
        val otherDuration = other.totalDuration(typ)
        taskCounts(typ) = taskCounts.getOrElseUpdate(typ, 0) + otherCount
        taskDurations(typ) = taskDurations.getOrElseUpdate(typ, Duration.Zero) + otherDuration
      }
      count += 1
    }
  }*/

  /**
   * @return true if no [[ProfilerTask]]s have been executed in this phase, false otherwise
   */
  override def isEmpty: Boolean = phaseTask.isEmpty

  override def nonEmpty: Boolean = phaseTask.nonEmpty

  def wasExecuted: Boolean = info.wasExecuted

  /**
   * @return true if a task of the given [[ProfilerTask]] type was executed in this phase
   */
  def wasExecuted(taskType: ProfilerTask): Boolean = taskStats.get(taskType).exists(_.count != 0)

  /**
   * @return the sum of all task durations of the given type
   */
  def totalDuration(taskType: ProfilerTask): Duration =
    taskStats.get(taskType).map(_.totalTime).getOrElse(Duration.Zero)


  /**
   * @return how many tasks of the given type were executed in this phase
   */
  def getCount(taskType: ProfilerTask): Int = taskStats.get(taskType).map(_.count).getOrElse(0)

  /**
   * @return the average duration of all [[ProfilerTask]]
   */
  def meanDuration(taskType: ProfilerTask): Duration =
    taskStats.get(taskType).map(_.meanDuration).getOrElse(Duration.Zero)

  /**
   * @return the duration of all [[ProfilerTask]] executed in the phase relative to the total
   *         phase duration
   */
  def totalRelativeDuration(taskType: ProfilerTask): Double = {
    val duration = totalDuration(taskType)
    if (duration == Duration.Zero) {
      0d
    } else {
      // sanity check for broken profile files
      requireState(
        totalDuration != Duration.Zero,
        s"Profiler tasks of type $taskType have non-zero duration " +
            s"$duration in phase $phase but the phase itself has zero duration. Most likely the profile file is broken.")
      duration.inNanoseconds.toDouble / totalDuration.inNanoseconds
    }
  }

  /**
   * Iterator over all [[ProfilerTask]]s that were executed at least once and have a total
   * duration greater than 0.
   */
  override def iterator: Iterator[ProfilerTask] =
    taskStats.iterator.filter { case (_, attr) =>
      attr.totalTime > Duration.Zero && attr.count > 0
    }.map(_._1)
}