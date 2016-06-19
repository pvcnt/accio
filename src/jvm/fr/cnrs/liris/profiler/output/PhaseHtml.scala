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

package fr.cnrs.liris.profiler.output

import java.io.PrintStream

import com.twitter.util.Duration
import fr.cnrs.liris.profiler.ProfilePhase
import fr.cnrs.liris.profiler.output.HtmlPrinter._
import fr.cnrs.liris.profiler.statistics.{PhaseStatistics, PhaseSummaryStatistics}

/**
 * Output [[PhaseSummaryStatistics]] and [[PhaseStatistics]] in HTML format.
 */
class PhaseHtml(phaseSummaryStats: PhaseSummaryStatistics, phaseStatistics: Map[ProfilePhase, PhaseStatistics]) {
  /*, Optional<CriticalPathStatistics> critPathStats, Optional<Integer> missingActionsCount*/
  /**
   * Output a style tag with all necessary CSS directives
   */
  def printCss(out: PrintStream): Unit = {
    out.println("<style type=\"text/css\"><!--")
    out.println("div.phase-statistics {")
    out.println("  margin: 0 10;")
    out.println("  font-size: small;")
    out.println("  font-family: monospace;")
    out.println("  float: left;")
    out.println("}")
    out.println("table.phase-statistics {")
    out.println("  border: 0px; text-align: right;")
    out.println("}")
    out.println("table.phase-statistics td {")
    out.println("  padding: 0 5;")
    out.println("}")
    out.println("td.left {")
    out.println("  text-align: left;")
    out.println("}")
    out.println("td.center {")
    out.println("  text-align: center;")
    out.println("}")
    out.println("--></style>")
  }

  /**
   * Print tables from [[phaseSummaryStats]] and [[phaseStatistics]] side by side.
   */
  def print(out: PrintStream): Unit = {
    printPhaseSummaryStatistics(out)
    Seq(ProfilePhase.Init)
        .map(phaseStatistics.apply)
        .filter(_.wasExecuted)
        .foreach(printPhaseStatistics(out, _))
    printExecutionPhaseStatistics(out)
    out.element("div", "style", "clear: both;")
  }

  /**
   * Print header and tables for a single phase.
   */
  private def printPhaseStatistics(out: PrintStream, phaseStat: PhaseStatistics) =
    out.tag("div", "class", "phase-statistics") {
      out.element("h3", s"${phaseStat.phase.name.capitalize} Phase Information")
      out.tag("table", "class", "phase-statistics") {
        printTwoColumnStatistic(out, s"Total ${phaseStat.phase.name} time", phaseStat.phaseDuration)
        printTimingDistribution(out, phaseStat)
      }
    }

  private def printExecutionPhaseStatistics(out: PrintStream): Unit =
    phaseStatistics.get(ProfilePhase.Exec).filter(_.wasExecuted).foreach { execPhase =>
      out.tag("div", "class", "phase-statistics") {
        out.element("h3", s"${execPhase.phase.name.capitalize} Phase Information")
        out.tag("table", "class", "phase-statistics") {
          printTwoColumnStatistic(out, s"Total ${execPhase.phase} time", execPhase.phaseDuration)
          phaseStatistics.get(ProfilePhase.Finish).filter(_.wasExecuted).foreach { phaseStat =>
            printTwoColumnStatistic(out, s"Total ${phaseStat.phase} time", phaseStat.phaseDuration)
          }
          printTwoColumnStatistic(out, "Actual execution time", execPhase.phaseDuration)

          /*CriticalPathHtml criticalPaths = null;
          if (criticalPathStatistics.isPresent()) {
            criticalPaths = new CriticalPathHtml(out, criticalPathStatistics.get(), execTime);
            criticalPaths.printTimingBreakdown();
          }*/
          printTimingDistribution(out, execPhase)
        }

        /*if (criticalPathStatistics.isPresent()) {
          criticalPaths.printCriticalPaths();
        }*/

        /*if (missingActionsCount.isPresent() && missingActionsCount.get() > 0) {
        lnOpen("p");
        println(missingActionsCount.get());
        print(
          " action(s) are present in the"
              + " action graph but missing instrumentation data. Most likely the profile file"
              + " has been created during a failed or aborted build.");
        lnClose();
      }*/
      }
    }

  /**
   * Print the table rows for the [[fr.cnrs.liris.profiler.ProfilerTask]] types and
   * their execution times.
   */
  private def printTimingDistribution(out: PrintStream, phaseStat: PhaseStatistics): Unit =
    if (phaseStat.nonEmpty) {
      out.tag("tr") {
        out.element("td", "class", "left", "colspan", "4", "Total time (across all threads) spent on:")
      }
      out.tag("tr") {
        out.element("th", "Type")
        out.element("th", "Total")
        out.element("th", "Count")
        out.element("th", "Average")
      }
      phaseStat.foreach { taskType =>
        out.tag("tr", "class", "phase-task-statistics") {
          out.element("td", taskType)
          out.element("td", PhaseUtils.prettyPercentage(phaseStat.totalRelativeDuration(taskType)))
          out.element("td", phaseStat.getCount(taskType))
          out.element("td", PhaseUtils.prettyTime(phaseStat.meanDuration(taskType)))
        }
      }
    }

  /**
   * Print a table for the phase overview with runtime and runtime percentage per phase and total.
   */
  private def printPhaseSummaryStatistics(out: PrintStream): Unit = {
    out.tag("div", "class", "phase-statistics") {
      out.element("h3", "Phase Summary Information")
      out.tag("table", "class", "phase-statistics") {
        phaseSummaryStats.foreach { phase =>
          out.tag("tr") {
            out.tag("td", "class", "left") {
              out.print(s"Total ${phase.name} phase time")
            }
            out.element("td", PhaseUtils.prettyTime(phaseSummaryStats.duration(phase)))
            out.element("td", PhaseUtils.prettyPercentage(phaseSummaryStats.relativeDuration(phase)))
          }
        }
        out.tag("tr") {
          out.element("td", "class", "left", "Total run time")
          out.element("td", PhaseUtils.prettyTime(phaseSummaryStats.totalDuration))
          out.element("td", "100.00%")
        }
      }
    }
  }

  private def printTwoColumnStatistic(out: PrintStream, name: String, duration: Duration): Unit = {
    out.tag("tr") {
      out.element("td", "class", "left", "colspan", "3", name)
      out.element("td", PhaseUtils.prettyTime(duration))
    }
  }
}