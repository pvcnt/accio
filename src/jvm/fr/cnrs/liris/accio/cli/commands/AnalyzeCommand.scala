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

package fr.cnrs.liris.accio.cli.commands

import java.io.PrintStream
import java.nio.file.{Path, Paths}

import com.twitter.util.Duration
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.cli.{Command, Reporter}
import fr.cnrs.liris.common.flags.{Flag, FlagsProvider}
import fr.cnrs.liris.common.util.FileUtils
import fr.cnrs.liris.profiler.output.{HtmlCreator, PhaseText, PhaseUtils}
import fr.cnrs.liris.profiler.statistics.{PhaseStatistics, PhaseSummaryStatistics}
import fr.cnrs.liris.profiler.{ProfileInfo, ProfilePhase, ProfilerTask}

import scala.util.matching.Regex

case class AnalyzeCommandOpts(
    @Flag(name = "dump",
      help = "Output full profile data dump either in human-readable 'text' format or script-friendly 'raw' format.")
    dumpMode: String = "",
    @Flag(name = "html",
      help = "If present, an HTML file visualizing the tasks of the profiled build is created. " +
          "The name of the html file is the name of the profile file plus '.html'.")
    html: Boolean = false,
    @Flag(name = "html_chart")
    htmlChart: Boolean = true,
    @Flag(name = "html_histograms")
    htmlHistograms: Boolean = true,
    @Flag(name = "task_tree")
    taskTree: String = "",
    @Flag(name = "task_tree_threshold")
    taskTreeThreshold: Duration = Duration.Zero
)

@Command(
  name = "analyze",
  help = "Analyze previously generated profiling data",
  allowResidue = true)
class AnalyzeCommand extends AccioCommand[AnalyzeCommandOpts] with StrictLogging {

  def run(flags: FlagsProvider, out: Reporter): ExitCode = {
    val opts = flags.as[AnalyzeCommandOpts]
    flags.residue.foreach { str =>
      val path = Paths.get(FileUtils.replaceHome(str))
      val info = ProfileInfo.read(path)
      handleProfileInfo(info, path, opts)
    }
    ExitCode.Success
  }

  private def handleProfileInfo(info: ProfileInfo, profilePath: Path, opts: AnalyzeCommandOpts): Unit = {
    if (opts.taskTree.nonEmpty) {
      printTaskTree(Console.out, info, profilePath, opts.taskTree.r, opts.taskTreeThreshold)
    } else if (opts.dumpMode.nonEmpty) {
      dumpProfile(info, Console.out, opts.dumpMode)
    } else if (opts.html) {
      HtmlCreator.write(
        info,
        profilePath.resolveSibling(profilePath.getFileName.toString + ".html"),
        generateChart = opts.htmlChart,
        generateHistograms = opts.htmlHistograms)
    } else {
      val phaseSummaryStats = PhaseSummaryStatistics(info)
      val phaseStats = ProfilePhase.values.map { phase => phase -> new PhaseStatistics(phase, info) }.toMap
      new PhaseText(phaseSummaryStats, phaseStats).print(Console.out)
    }
  }

  /**
   * Prints trees rooted at tasks with a description matching a pattern.
   */
  private def printTaskTree(out: PrintStream, info: ProfileInfo, profilePath: Path, regex: Regex, taskDurationThreshold: Duration) = {
    val tasks = info.tasksByDescription(regex)
    if (tasks.isEmpty) {
      out.println(s"No tasks matching $regex found in profile path ${profilePath.toAbsolutePath}.")
    } else {
      var skipped = 0
      tasks.foreach { task =>
        if (!task.printTaskTree(out, taskDurationThreshold)) {
          skipped += 1
        }
      }
      if (skipped > 0) {
        out.print(s"Skipped $skipped matching task(s) below the duration threshold.")
      }
      out.println()
    }
  }

  /**
   * Dumps all tasks in the requested format.
   */
  private def dumpProfile(info: ProfileInfo, out: PrintStream, dumpMode: String) = {
    dumpMode match {
      case "raw" => info.tasks.foreach(dumpRaw(_, out))
      case "text" => info.rootTasks.foreach(dumpText(_, out, 0))
      case str => throw new IllegalArgumentException(s"Unknown mode '$str'")
    }
  }

  /**
   * Dumps the task information and all subtasks.
   */
  private def dumpText(task: ProfileInfo#Task, out: PrintStream, indent: Int): Unit = {
    val sb = new StringBuilder(
      "\n%s %s\nThread: %-6d  Id: %-6d  Parent: %d\nStart time: %-12s   Duration: %s".format(
        task.typ,
        task.description,
        task.threadId,
        task.id,
        task.parentId.getOrElse(0),
        PhaseUtils.prettyTime(task.startTime),
        PhaseUtils.prettyTime(task.duration)))
    if (task.hasStats) {
      sb.append("\n")
      ProfilerTask.values.foreach { typ =>
        task.stats.get(typ).foreach { attr =>
          sb.append(typ.toString.toLowerCase)
              .append("=(").
              append(attr.count)
              .append(", ").
              append(PhaseUtils.prettyTime(attr.totalTime))
              .append(") ");
        }
      }
    }
    out.println(sb.toString.replace("\n", "\n" + " " * indent))
    task.subtasks.foreach { subtask =>
      dumpText(subtask, out, indent + 1)
    }
  }

  private def dumpRaw(task: ProfileInfo#Task, out: PrintStream): Unit = {
    val sb = new StringBuilder()
    ProfilerTask.values.foreach { typ =>
      task.stats.get(typ).foreach { attr =>
        sb.append(typ.toString.toLowerCase())
            .append(",").
            append(attr.count)
            .append(",")
            .append(attr.totalTime)
            .append(" ");
      }
    }
    out.println(Seq(task.threadId,
      task.id,
      task.parentId,
      task.startTime.inNanoseconds,
      task.duration.inNanoseconds,
      sb.toString.trim,
      task.typ,
      task.description).mkString("|"))
  }
}