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

import java.io.{BufferedOutputStream, IOException, OutputStream}
import java.util
import java.util.PriorityQueue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.{Deflater, DeflaterOutputStream}

import com.twitter.conversions.time._
import com.twitter.util.{Duration, JavaTimer, Time}
import fr.cnrs.liris.common.util.Requirements._
import org.apache.thrift.protocol.TCompactProtocol
import org.apache.thrift.transport.TIOStreamTransport

import scala.collection.JavaConverters._
import scala.collection.mutable

private[profiler] class ProfilerSession(profiledTaskKinds: ProfiledTaskKinds, stream: Option[OutputStream], recordAllDurations: Boolean,
    comment: Option[String], profileStartTime: Time) {
  private[this] var saveException: Option[Throwable] = None
  private[this] val taskStack = new TaskStack
  private[this] val taskQueue = new ConcurrentLinkedQueue[TaskData]
  private[this] val describer = new ObjectDescriber
  private[this] val taskId = new AtomicInteger(0)
  private[this] val slowestTasks = ProfilerTask.values
      .filter(_.collectsSlowestInstances)
      .map(typ => typ.id -> new SlowestTaskAggregator(typ.slowestInstancesCount))
      .toMap
  private[this] val tasksHistograms = ProfilerTask.values
      .map(typ => typ.id -> new SingleStatRecorder(typ, ProfilerSession.HistogramBuckets))
      .toMap
  private[this] val protocol = stream.map { stream =>
    // Wrapping deflater stream in the buffered stream proved to reduce CPU consumption caused by
    // the save() method. Values for buffer sizes were chosen by running small amount of tests
    // and identifying point of diminishing returns - but I have not really tried to optimize
    // them.
    val outputStream = new BufferedOutputStream(new DeflaterOutputStream(stream,
      new Deflater(Deflater.BEST_SPEED), ProfilerSession.DeflatedBufferSize), ProfilerSession.BufferSize)
    new TCompactProtocol.Factory().getProtocol(new TIOStreamTransport(outputStream))
  }
  protocol.foreach { protocol =>
    thrift.StartProfile(profileStartTime.inNanoseconds, comment).write(protocol)
  }
  private[this] val timer = protocol.map { _ =>
    new JavaTimer(isDaemon = true, Some("ProfilerTimer"))
  }
  timer.foreach { timer =>
    // Start save thread
    timer.schedule(Time.now + ProfilerSession.SaveDelay, ProfilerSession.SaveDelay) {
      save()
    }
  }

  def getSlowestTasks: Iterable[SlowTask] = synchronized {
    slowestTasks.values.map(_.slowestTasks).reduce(_ ++ _)
  }

  def getTasksHistograms: Seq[StatRecorder] = tasksHistograms.values.toSeq

  def isProfiling(typ: ProfilerTask): Boolean = profiledTaskKinds.isProfiling(typ)

  /**
   * Disable profiling and complete profile file creation.
   * Subsequent calls to beginTask/endTask will no longer
   * be recorded in the profile.
   */
  def stop(): Unit = synchronized {
    saveException.foreach(throw _)

    // Log a final event to update the duration of the last phase.
    logTask(Time.now, Duration.Zero, ProfilerTask.Info, "Finishing")

    // Disable the profiling and flush a last time all pending tasks.
    save()
    saveException.foreach(throw _)

    // Write the end of profile.
    protocol.foreach { protocol =>
      //TODO: include slow tasks.
      protocol.writeI32(ProfilerSession.EndMarker)
      val end = thrift.EndProfile(describer.descriptions)
      end.write(protocol)
      protocol.getTransport.close()
    }
  }

  def startTask(typ: ProfilerTask, obj: Any): Unit = {
    // ProfilerInfo.allTasksById is supposed to be an id -> Task map, but it is in fact a List,
    // which means that we cannot drop tasks to which we had already assigned ids. Therefore,
    // non-leaf tasks must not have a minimum duration. However, we don't quite consistently
    // enforce this, and Blaze only works because we happen not to add child tasks to those parent
    // tasks that have a minimum duration.
    if (isProfiling(typ)) {
      taskStack.push(typ, obj)
    }
  }

  def completeTask(typ: ProfilerTask): Unit = {
    if (isProfiling(typ)) {
      val endTime = Time.now
      val data = taskStack.pop()
      requireState(data.typ == typ, s"Inconsistent Profiler.completeTask() call for the $typ task.\n $taskStack")
      data.duration = endTime - data.startTime
      taskStack.peek().foreach(_.aggregateChild(typ, data.duration))
      val shouldRecordTask = wasTaskSlowEnoughToRecord(typ, data.duration)
      if (protocol.isDefined && (shouldRecordTask || data.hasChildren)) {
        taskQueue.add(data)
      }
      if (shouldRecordTask) {
        slowestTasks.get(typ.id).foreach(_.add(data))
      }
    }
  }

  def kill(): Unit = {
    timer.foreach(_.stop())
  }

  /**
   * Saves all gathered information from taskQueue queue to the file.
   * Method is invoked internally by the Timer-based thread and at the end of
   * profiling session.
   */
  private def save(): Unit = synchronized {
    protocol.foreach { protocol =>
      try {
        while (!taskQueue.isEmpty) {
          val taskData = taskQueue.poll()

          val children = taskData.counts.map { case (id, count) =>
            thrift.AggregatedStats(id, count, taskData.durations(id))
          }.toSeq

          // To save space (and improve performance), convert all description
          // strings to the canonical object and use IdentityHashMap to assign
          // unique numbers for each string.
          val descriptionIndex = describer.index(taskData.obj)

          val profile = thrift.TaskProfile(
            taskData.threadId,
            taskData.id,
            taskData.parentId,
            (taskData.startTime - profileStartTime).inNanoseconds,
            taskData.duration.inNanoseconds,
            descriptionIndex,
            taskData.typ.id,
            children
          )
          protocol.writeI32(ProfilerSession.TaskMarker)
          profile.write(protocol)
        }
      } catch {
        case t: Throwable =>
          saveException = Some(t)
          Profiler.kill()
          try {
            protocol.getTransport.close()
          } catch {
            case _: IOException => // Ignore it
          }
      }
    }
  }

  /**
   * Add a task directly to the main queue bypassing the task stack. Used for simple tasks that are
   * known to not have any subtasks.
   *
   * @param startTime Task start time
   * @param duration  Task duration
   * @param typ       Task type
   * @param obj       An object associated with that task. Can be String object that describes it.
   */
  def logTask(startTime: Time, duration: Duration, typ: ProfilerTask, obj: Any): Unit = {
    if (!isProfiling(typ)) {
      return
    }
    requireState(typ != ProfilerTask.Phase || taskStack.isEmpty, "Phase tasks must not be nested")
    // It can happen that System.nanoTime return non increasing values.
    val actualDuration = duration.max(Duration.Zero)

    tasksHistograms(typ.id).addStat(duration, obj)
    val parent = taskStack.peek()
    parent.foreach(_.aggregateChild(typ, actualDuration))
    if (wasTaskSlowEnoughToRecord(typ, actualDuration)) {
      val data = taskStack.create(startTime, typ, obj, actualDuration)
      protocol.foreach(_ => taskQueue.add(data))
      slowestTasks.get(typ.id).foreach(_.add(data))
    }
  }

  /**
   * Unless `recordAllDurations` is set we drop small tasks and add their time to the parents
   * duration.
   */
  private def wasTaskSlowEnoughToRecord(typ: ProfilerTask, duration: Duration): Boolean =
    recordAllDurations || duration >= typ.minDuration

  /**
   * Container for the single task record.
   * Should never be instantiated directly - use TaskStack.create() instead.
   *
   * Class itself is not thread safe, but all access to it from Profiler
   * methods is.
   */
  private class TaskData(val startTime: Time, parent: Option[TaskData], val typ: ProfilerTask, val obj: Any, var duration: Duration) {
    // number of invocations per ProfilerTask type
    private[this] val _counts = mutable.Map.empty[Int, Int]
    // time spent in the task per ProfilerTask type
    private[this] val _durations = mutable.Map.empty[Int, Long]
    val threadId = Thread.currentThread().getId
    val id = taskId.incrementAndGet()

    def parentId: Option[Int] = parent.map(_.id)

    def counts: Map[Int, Int] = _counts.toMap

    def durations: Map[Int, Long] = _durations.toMap

    def hasChildren: Boolean = _counts.nonEmpty

    /**
     * Aggregates information about an *immediate* subtask.
     */
    def aggregateChild(typ: ProfilerTask, duration: Duration): Unit = {
      if (_counts.contains(typ.id)) {
        _counts(typ.id) += 1
        _durations(typ.id) += duration.inNanoseconds
      } else {
        _counts(typ.id) = 1
        _durations(typ.id) = duration.inNanoseconds
      }
    }

    override def toString: String = s"Thread $threadId, task $id, type $typ, $obj"
  }

  /**
   * Tracks nested tasks for each thread.
   *
   * java.util.ArrayDeque is the most efficient stack implementation in the
   * Java Collections Framework (java.util.Stack class is older synchronized
   * alternative). It is, however, used here strictly for LIFO operations.
   * However, ArrayDeque is 1.6 only. For 1.5 best approach would be to utilize
   * ArrayList and emulate stack using it.
   */
  private class TaskStack extends ThreadLocal[mutable.ListBuffer[TaskData]] {
    override def initialValue(): mutable.ListBuffer[TaskData] = {
      mutable.ListBuffer.empty
    }

    def peek(): Option[TaskData] = get().lastOption

    def pop(): TaskData = {
      val list = get()
      list.remove(list.size - 1)
    }

    def isEmpty: Boolean = get().isEmpty

    def nonEmpty: Boolean = get().nonEmpty

    def push(eventType: ProfilerTask, obj: Any): Unit =
      get() ++= Seq(create(Time.now, eventType, obj))

    def create(startTime: Time, eventType: ProfilerTask, obj: Any, duration: Duration = Duration.Zero): TaskData =
      new TaskData(startTime, peek(), eventType, obj, duration)

    def clear(): Unit = get().clear()

    override def toString: String = {
      val sb = new StringBuilder(s"Current task stack for thread ${Thread.currentThread().getName}:\n")
      get().reverseIterator.foreach(task => sb.append(s"$task\n"))
      sb.toString
    }
  }

  /**
   * Implements datastore for object description indices. Intended to be used
   * only by the Profiler.save() method.
   */
  private class ObjectDescriber {
    private[this] val descMap = new util.IdentityHashMap[Any, (Int, String)](2000).asScala

    def index(obj: Any): Int = descMap.get(obj) match {
      case Some((idx, _)) => idx
      case None =>
        var description = Describable.describe(obj)
        if (description.length > 20000) {
          // Truncate too long descriptions.
          description = description.substring(0, 20000)
        }
        val idx = descMap.size
        descMap(obj) = (idx, description)
        idx
    }

    def descriptions: Seq[String] = descMap.values.toSeq.sortBy(_._1).map(_._2)

    def clear(): Unit = {
      descMap.clear()
    }
  }

  /**
   * Aggregator class that keeps track of the slowest tasks of the specified type.
   */
  private class SlowestTaskAggregator(size: Int) {
    /**
     * priorityQueues is sharded so that all threads need not compete for the same
     * lock if they do the same operation at the same time. Access to the individual queues is
     * synchronized on the queue objects themselves.
     */
    private[this] val Shards = 16
    private[this] val priorityQueues = Seq.fill(Shards)(new util.PriorityQueue[SlowTask](size + 1))

    def add(taskData: TaskData): Unit = {
      val queue = priorityQueues((Thread.currentThread().getId % Shards).toInt)
      queue synchronized {
        if (queue.size == size) {
          // Optimization: check if we are faster than the fastest element. If we are, we would
          // be the ones to fall off the end of the queue, therefore, we can safely return early.
          if (queue.peek().duration > taskData.duration) {
            return
          }
          queue.add(new SlowTask(taskData.obj, taskData.duration, taskData.typ))
          queue.remove()
        } else {
          queue.add(new SlowTask(taskData.obj, taskData.duration, taskData.typ))
        }
      }
    }

    def clear(): Unit =
      priorityQueues.foreach { queue =>
        queue synchronized {
          queue.clear()
        }
      }

    def slowestTasks: Iterable[SlowTask] = {
      // This is slow, but since it only happens during the end of the invocation, it's OK
      val merged = new PriorityQueue[SlowTask](size * Shards)
      priorityQueues.foreach { queue =>
        queue synchronized {
          merged.addAll(queue)
        }
      }
      while (merged.size > size) {
        merged.remove()
      }
      merged.asScala
    }
  }

}

private object ProfilerSession {
  // Interval at which the profiler will check for gathered data and persist all of it.
  private val SaveDelay = 2000 milliseconds
  private val HistogramBuckets = 20

  val TaskMarker = 0x01
  val EndMarker = 0x02

  val DeflatedBufferSize = 65536
  val BufferSize = 262144
}