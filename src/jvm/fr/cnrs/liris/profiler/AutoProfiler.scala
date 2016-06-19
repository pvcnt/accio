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

import com.twitter.util.{Time, Duration}
import org.slf4j.Logger

/**
 * A convenient way to actively get access to timing information (e.g. for logging and/or
 * profiling purposes) with minimal boilerplate.
 */
object AutoProfiler {
  def profile[T](obj: Any, typ: ProfilerTask)(fn: => T): T = {
    Profiler.startTask(typ, obj)
    val res = fn
    Profiler.completeTask(typ)
    res
  }

  def time[T](obj: Any, logger: Logger)(fn: => T): T = {
    val taskDescription = Describable.describe(obj)
    time(new LoggingElapsedTimeReceiver(taskDescription, logger))(fn)
  }

  def time[T](elapsedTimeReceiver: ElapsedTimeReceiver)(fn: => T): T = {
    val startTime = Time.now
    val res = fn
    elapsedTimeReceiver.accept(Time.now - startTime)
    res
  }
}

/**
 * An opaque receiver of elapsed time information.
 */
trait ElapsedTimeReceiver {
  /**
   * Receives the elapsed time of the lifetime of an [[AutoProfiler]] section.
   *
   * Note that System#nanoTime isn't guaranteed to be non-decreasing, so implementations should
   * check for non-positive `elapsedTimeNanos` if they care about this sort of thing.
   *
   * @param elapsedTime Elapsed time
   */
  def accept(elapsedTime: Duration): Unit
}

private class LoggingElapsedTimeReceiver(taskDescription: String, logger: Logger) extends ElapsedTimeReceiver {
  override def accept(elapsedTime: Duration): Unit = {
    if (elapsedTime > Duration.Zero) {
      logger.info(s"Spent $elapsedTime doing $taskDescription")
    }
  }
}