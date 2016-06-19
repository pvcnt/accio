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

import java.io.OutputStream
import java.util.concurrent.atomic.AtomicReference

import com.twitter.util.{Duration, Time}

object Profiler {
  private[this] val session = new AtomicReference[ProfilerSession]

  /**
   * Enable profiling.
   *
   * Subsequent calls to beginTask/endTask will be recorded in the provided output stream. Please
   * note that stream performance is extremely important and buffered streams should be utilized.
   *
   * @param profiledTaskKinds  which kinds of [[ProfilerTask]]s to track
   * @param stream             output stream to store profile data. Note: passing unbuffered stream object
   *                           reference may result in significant performance penalties
   * @param recordAllDurations iff true, record all tasks regardless of their duration; otherwise
   *                           some tasks may get aggregated if they finished quick enough
   * @param comment            a comment to insert in the profile data
   * @param execStartTime      execution start time
   */
  def start(profiledTaskKinds: ProfiledTaskKinds, stream: Option[OutputStream], recordAllDurations: Boolean = false,
      comment: Option[String] = None, execStartTime: Time = Time.now): Unit = {
    if (!session.compareAndSet(null, new ProfilerSession(profiledTaskKinds, stream, recordAllDurations, comment, execStartTime))) {
      throw new IllegalStateException("Profiler is already active")
    }
  }

  def getSlowestTasks: Iterable[SlowTask] =
    Option(session.get) match {
      case Some(s) => s.getSlowestTasks
      case None => Iterable.empty
    }

  def getTasksHistograms: Seq[StatRecorder] =
    Option(session.get) match {
      case Some(s) => s.getTasksHistograms
      case None => Seq.empty
    }


  def isActive: Boolean = Option(session.get).isDefined

  def kill(): Unit =
    Option(session.getAndSet(null)) match {
      case Some(s) => s.kill()
      case None => throw new IllegalStateException("Profiler is not active")
    }

  /**
   * Disable profiling and complete profile file creation.
   * Subsequent calls to beginTask/endTask will no longer
   * be recorded in the profile.
   */
  def stop(): Unit =
    Option(session.getAndSet(null)) match {
      case Some(s) =>
        s.kill()
        s.stop()
      case None => throw new IllegalStateException("Profiler is not active")
    }

  /**
   * Used externally to submit simple task (one that does not have any subtasks).
   * Depending on the minDuration attribute of the task type, task may be
   * just aggregated into the parent task and not stored directly.
   *
   * @param startTime task start time
   * @param typ       task type
   * @param obj       object associated with that task. Can be String object that
   *                  describes it.
   */
  def logSimpleTask(startTime: Time, typ: ProfilerTask, obj: Any): Unit =
    logSimpleTask(startTime, Time.now, typ, obj)

  /**
   * Used externally to submit simple task (one that does not have any
   * subtasks). Depending on the minDuration attribute of the task type, task
   * may be just aggregated into the parent task and not stored directly.
   *
   * Note that start and stop time must both be acquired from the same clock
   * instance.
   *
   * @param startTime task start time
   * @param stopTime  task stop time
   * @param typ       task type
   * @param obj       object associated with that task. Can be String object that
   *                  describes it.
   */
  def logSimpleTask(startTime: Time, stopTime: Time, typ: ProfilerTask, obj: Any): Unit =
    logSimpleTask(startTime, stopTime - startTime, typ, obj)

  /**
   * Used externally to submit simple task (one that does not have any
   * subtasks). Depending on the minDuration attribute of the task type, task
   * may be just aggregated into the parent task and not stored directly.
   *
   * @param startTime task start time (obtained through { @link
   *                  Profiler#nanoTimeMaybe()})
   * @param duration  the duration of the task
   * @param typ       task type
   * @param obj       object associated with that task. Can be String object that
   *                  describes it.
   */
  def logSimpleTask(startTime: Time, duration: Duration, typ: ProfilerTask, obj: Any): Unit =
    Option(session.get) match {
      case Some(s) => s.logTask(startTime, duration, typ, obj)
      case None =>
    }

  /**
   * Used to log "events" - tasks with zero duration.
   */
  def logEvent(typ: ProfilerTask, obj: Any): Unit = logSimpleTask(Time.now, Duration.Zero, typ, obj)

  /**
   * Records the beginning of the task specified by the parameters. This method
   * should always be followed by completeTask() invocation to mark the end of
   * task execution (usually ensured by try {} finally {} block). Failure to do
   * so will result in task stack corruption.
   *
   * Use of this method allows to support nested task monitoring. For tasks that
   * are known to not have any subtasks, logSimpleTask() should be used instead.
   *
   * @param typ A task type
   * @param obj An object associated with that task. Can be String object that describes it.
   */
  def startTask(typ: ProfilerTask, obj: Any): Unit =
    Option(session.get) match {
      case Some(s) => s.startTask(typ, obj)
      case None =>
    }

  /**
   * Records the end of the task and moves tasks from the thread-local stack to
   * the main queue. Will validate that given task type matches task at the top
   * of the stack.
   *
   * @param typ A task type
   */
  def completeTask(typ: ProfilerTask): Unit = Option(session.get) match {
    case Some(s) => s.completeTask(typ)
    case None =>
  }

  /**
   * Convenience method to log phase marker tasks.
   */
  def markPhase(phase: ProfilePhase): Unit = {
    MemoryProfiler.markPhase(phase)
    logEvent(ProfilerTask.Phase, phase.description)
  }

  /**
   * Convenience method to log spawn tasks.
   *
   * TODO(bazel-team): Right now method expects single string of the spawn action
   * as task description (usually either argv[0] or a name of the main executable
   * in case of complex shell commands). Maybe it should accept Command object
   * and create more user friendly description.
   */
  def logSpawn(startTime: Time, arg0: String): Unit =
    logSimpleTask(startTime, Time.now - startTime, ProfilerTask.Spawn, arg0)
}

/**
 * Which [[ProfilerTask]]s are profiled.
 */
trait ProfiledTaskKinds {
  /**
   * Whether the Profiler collects data for the given task type.
   */
  def isProfiling(typ: ProfilerTask): Boolean
}

object ProfiledTaskKinds {
  /**
   * Do not profile anything.
   *
   * Performance is best with this case, but we lose critical path analysis and slowest
   * operation tracking.
   */
  val None = new ProfiledTaskKinds {
    override def isProfiling(typ: ProfilerTask): Boolean = false
  }

  /**
   * Profile on a few, known-to-be-slow tasks.
   *
   * Performance is somewhat decreased in comparison to [[None]], but we still track the
   * slowest operations.
   */
  val Slowest = new ProfiledTaskKinds {
    override def isProfiling(typ: ProfilerTask): Boolean = typ.collectsSlowestInstances
  }

  /**
   * Profile all tasks.
   *
   * This is in use when `--profile` is specified.
   */
  val All = new ProfiledTaskKinds {
    override def isProfiling(typ: ProfilerTask): Boolean = true
  }
}

/**
 * A task that was very slow.
 */
class SlowTask(obj: Any, val duration: Duration, val typ: ProfilerTask) extends Ordered[SlowTask] {
  def description: String = Describable.describe(obj)

  override def compare(other: SlowTask): Int = duration.compare(other.duration)
}