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

import java.io.{FileInputStream, IOException, PrintStream}
import java.nio.file.Path
import java.util.zip.{Inflater, InflaterInputStream}

import com.twitter.util.{Duration, Time}
import org.apache.thrift.protocol.TCompactProtocol
import org.apache.thrift.transport.TIOStreamTransport
import fr.cnrs.liris.common.util.Requirements._
import scala.collection.mutable
import scala.util.control.NonFatal
import scala.util.matching.Regex

/**
 * Holds parsed profile file information and provides various ways of
 * accessing it (mostly through different dictionaries or sorted lists).
 */
class ProfileInfo private(
  startTime: Time,
  val comment: Option[String],
  descriptions: Seq[String],
  data: Seq[thrift.TaskProfile]) {
  // All tasks.
  private[this] val allTasks = data.map(decode).sorted
  // Mapping between tasks and their subtasks
  private[this] val subtasksMap = allTasks.groupBy(_.parentId)
  // Root tasks (with no parent task).
  val rootTasks = subtasksMap.getOrElse(None, Seq.empty)
  // Tasks marking a new phase.
  private[this] val phaseTasks = rootTasks.filter(_.typ == ProfilerTask.Phase)

  class CompactStats(map: Map[Int, AggregateAttr]) {
    def get(typ: ProfilerTask): Option[AggregateAttr] = map.get(typ.id)

    def apply(typ: ProfilerTask): AggregateAttr = map(typ.id)

    def nonEmpty: Boolean = map.nonEmpty

    def toMap: Map[Int, AggregateAttr] = map

    /**
     * Returns cumulative time stored in this instance across whole
     * ProfilerTask dimension.
     */
    def totalTime: Duration = map.values.map(_.totalTime).foldLeft(Duration.Zero)(_ + _)
  }

  class Task(
    val threadId: Long,
    val id: Int,
    val parentId: Option[Int],
    val startTime: Duration,
    val duration: Duration,
    val typ: ProfilerTask,
    val stats: CompactStats,
    descIndex: Int) extends Ordered[Task] {

    def isRoot: Boolean = parentId.isEmpty

    /**
     * Aggregated statistics covering all subtasks (including nested ones). Should be called only
     * on root tasks.
     *
     * @throws IllegalArgumentException If this task is not a root one
     */
    def aggregatedStats: CompactStats = {
      require(parentId.isEmpty, s"Aggregated statistics should only be computed for root tasks (task $id is a child of ${parentId.get})")
      val attrs = mutable.Map.empty[Int, AggregateAttr]
      combineStats(attrs)
      new CompactStats(attrs.toMap)
    }

    def nextPhaseTask: Option[Task] = {
      val idx = phaseTasks.indexWhere(_.id == id)
      if (idx > -1 && idx < phaseTasks.size - 1) {
        Some(phaseTasks(idx + 1))
      } else {
        None
      }
    }

    def subtasks: Seq[Task] = subtasksMap.getOrElse(Some(id), Seq.empty)

    def description: String = descriptions(descIndex)

    def hasStats: Boolean = stats.nonEmpty

    def inheritedDuration: Duration = stats.totalTime

    /**
     * Produce a nicely indented tree of the task and its subtasks with execution time.
     *
     * @param durationThreshold Tasks with a shorter duration than this threshold will be
     *                          skipped
     * @return whether this task took longer than the threshold and was thus printed
     */
    def printTaskTree(out: PrintStream, durationThreshold: Duration): Boolean =
      printTaskTree(out, "", durationThreshold)

    private def printTaskTree(out: PrintStream, indent: String, durationThreshold: Duration): Boolean = {
      if (duration < durationThreshold) {
        return false
      }
      out.print("%s%6d %s".format(indent, id, typ))
      out.print(" (%5.3f ms) ".format(duration.inNanoseconds / 1000000d))
      out.print(description)

      out.print(" [")
      val sortedSubTasks = subtasks.sortBy(_.duration).reverse.filter(_.duration >= durationThreshold)
      var sep = ""
      sortedSubTasks.foreach { task =>
        out.print(sep)
        out.println()
        task.printTaskTree(out, s"$indent  ", durationThreshold)
        sep = ","
      }
      if (sortedSubTasks.nonEmpty) {
        out.println()
        out.print(indent)
      }
      val skipped = subtasks.length - sortedSubTasks.size
      if (skipped > 0) {
        out.print(s"$skipped subtree(s) omitted")
      }
      out.print("]")

      if (indent.isEmpty) {
        out.println()
      }
      true
    }

    override def equals(other: Any): Boolean = other match {
      case t: Task => t.id == id
      case _ => false
    }

    override def hashCode: Int = id

    override def toString: String = s"$typ($id,$description)"

    /**
     * Tasks records by default sorted by their id. Since id was obtained using
     * AtomicInteger, this comparison will correctly sort tasks in time-ascending
     * order regardless of their origin thread.
     */
    override def compare(other: Task): Int = id.compare(other.id)

    private def combineStats(attrs: mutable.Map[Int, AggregateAttr]): Unit = {
      val ownIndex = typ.id
      if (parentId.isDefined) {
        // Parent task already accounted for this task total duration. We need to adjust
        // for the inherited duration.
        attrs(ownIndex) += -inheritedDuration
      }
      stats.toMap.foreach { case (k, v) =>
        if (attrs.contains(k)) {
          attrs(k) += v
        } else {
          attrs(k) = v
        }
      }
      subtasks.foreach(_.combineStats(attrs))
    }
  }

  /**
   * Return all tasks this profile is composed of, in order of creation.
   */
  def tasks: Seq[Task] = allTasks

  def wasExecuted: Boolean = phaseTasks.nonEmpty

  /**
   * Calculates cumulative time attributed to the specific task type.
   * Expects to be called only for root (parentId = 0) tasks.
   * calculateStats() must have been called first.
   */
  def statsForType(typ: ProfilerTask, tasks: Seq[Task]): AggregateAttr = {
    var totalTime = Duration.Zero
    var count = 0
    tasks.foreach { task =>
      task.aggregatedStats.get(typ).foreach { attr =>
        count += attr.count
        totalTime += attr.totalTime
      }
      if (task.typ == typ) {
        count += 1
        totalTime += (task.duration - task.inheritedDuration)
      }
    }
    AggregateAttr(count, totalTime)
  }

  /**
   * Return list of all root tasks related to (in other words, started during)
   * the specified phase task.
   */
  def tasksForPhase(phaseTask: Task): Seq[Task] = {
    require(phaseTask.typ == ProfilerTask.Phase, s"Unsupported task type ${phaseTask.typ}")

    // Algorithm below takes into account fact that rootTasksById list is sorted
    // by the task id and task id values are monotonically increasing with time
    // (this property is guaranteed by the profiler). Thus list is effectively
    // sorted by the startTime. We are trying to select a sublist that includes
    // all tasks that were started later than the given task but earlier than
    // its completion time.
    val startIndex = rootTasks.indexOf(phaseTask)
    requireState(startIndex >= 0, s"Phase task ${phaseTask.id} is not a root task")
    val endIndex = phaseTask.nextPhaseTask match {
      case Some(relatedTask) => rootTasks.indexOf(relatedTask)
      case None => rootTasks.size
    }
    requireState(endIndex >= startIndex, s"Failed to find end of the phase marked by the task ${phaseTask.id}")
    rootTasks.slice(startIndex, endIndex)
  }

  /**
   * Return task corresponding to a given phase.
   */
  def phaseTask(phase: ProfilePhase): Option[Task] =
    phaseTasks.find(_.description == phase.description)

  /**
   * Return duration of the given phase.
   */
  def phaseDuration(phaseTask: Task): Duration = {
    require(phaseTask.typ == ProfilerTask.Phase, s"Unsupported task type ${phaseTask.typ}")
    val duration = phaseTask.nextPhaseTask match {
      case Some(relatedTask) => relatedTask.startTime - phaseTask.startTime
      case None =>
        val lastTask = rootTasks.last
        lastTask.startTime - phaseTask.startTime + lastTask.duration
    }
    requireState(duration >= Duration.Zero, s"Negative duration: $duration")
    duration
  }

  /**
   * Searche for tasks by their description. Linear in the number of tasks.
   *
   * @param regex A regular expression pattern which will be matched against the task description
   * @return Tasks matching the description
   */
  def tasksByDescription(regex: Regex): Iterable[Task] =
    allTasks.filter(task => regex.findFirstIn(task.description).isDefined)

  private def decode(task: thrift.TaskProfile) = {
    val stats = new CompactStats(task.children.map { s =>
      s.id -> AggregateAttr(s.count, Duration.fromNanoseconds(s.duration))
    }.toMap)
    new Task(
      task.threadId,
      task.taskId,
      task.parentId,
      Duration.fromNanoseconds(task.startTime),
      Duration.fromNanoseconds(task.duration),
      ProfilerTask(task.typeIndex),
      stats,
      task.descriptionIndex)
  }
}

object ProfileInfo {
  /**
   * Read an Accio profile file.
   *
   * @param path Profile file path
   * @return A profile collected by the profiler
   * @throws IOException If the file is corrupted or incomple
   */
  @throws[IOException]
  def read(path: Path): ProfileInfo = {
    val inputStream = new InflaterInputStream(new FileInputStream(path.toFile), new Inflater, ProfilerSession.DeflatedBufferSize)
    val protocol = new TCompactProtocol.Factory().getProtocol(new TIOStreamTransport(inputStream))
    try {
      // First: preliminary information about the profile.
      //println(protocol.readStructBegin())
      val start = thrift.StartProfile.decode(protocol)
      // Next: all tasks, one after this other (after a ProfileMarker)
      val tasks = mutable.ArrayBuffer.empty[thrift.TaskProfile]
      var marker = protocol.readI32()
      while (marker != ProfilerSession.EndMarker) {
        tasks += thrift.TaskProfile.decode(protocol)
        marker = protocol.readI32()
      }
      // Last (after an EndMarker): indexed information about the profile (like descriptions).
      val end = thrift.EndProfile.decode(protocol)
      new ProfileInfo(Time.fromNanoseconds(start.startTime), start.comment, end.descriptions, tasks.toSeq)
    } catch {
      case NonFatal(e) =>
        throw new IOException(s"Profile file ${path.toAbsolutePath} may be corrupted or incomplete", e)
    }
  }
}

/**
 * Container for the aggregated stats.
 *
 * @param count
 * @param totalTime
 */
case class AggregateAttr(count: Int, totalTime: Duration) {
  def +(other: AggregateAttr): AggregateAttr =
    AggregateAttr(count + other.count, totalTime + other.totalTime)

  def +(duration: Duration): AggregateAttr = AggregateAttr(count, totalTime + duration)

  def meanDuration: Duration = totalTime / count
}

class CriticalPathEntry(val task: ProfileInfo#Task, duration: Duration, next: Option[CriticalPathEntry]) {
  val cumulativeDuration: Duration = duration + next.map(_.cumulativeDuration).getOrElse(Duration.Zero)
  var criticalTime: Duration = Duration.Zero

  /**
   * @return true when this is just an action element on the critical path and is thus a
   *         pre-processed and -analyzed critical path element
   */
  def isComponent: Boolean = task.typ == ProfilerTask.CriticalPathComponent
}