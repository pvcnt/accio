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

import fr.cnrs.liris.profiler.ProfilePhase
import fr.cnrs.liris.profiler.statistics.{PhaseStatistics, PhaseSummaryStatistics}

/**
 * Output [[PhaseSummaryStatistics]] and [[PhaseStatistics]] in text format.
 */
class PhaseText(phaseSummaryStats: PhaseSummaryStatistics, phaseStatistics: Map[ProfilePhase, PhaseStatistics]) {
  def print(out: PrintStream): Unit = {
    printPhaseSummaryStatistics(out)
    Seq(ProfilePhase.Init)
        .map(phaseStatistics.apply)
        .filter(_.wasExecuted)
        .foreach(printPhaseStatistics(out, _))
    printExecutionPhaseStatistics(out)
  }

  /**
   * Print a table for the phase overview with runtime and runtime percentage per phase and total.
   */
  private def printPhaseSummaryStatistics(out: PrintStream): Unit = {
    out.println("\n=== PHASE SUMMARY INFORMATION ===")
    phaseSummaryStats.foreach { phase =>
      val phaseDuration = phaseSummaryStats.duration(phase)
      val relativeDuration = phaseSummaryStats.relativeDuration(phase)
      printThreeColumns(out,
        s"Total ${phase.name} phase time",
        PhaseUtils.prettyTime(phaseDuration),
        PhaseUtils.prettyPercentage(relativeDuration))
    }
    printThreeColumns(out,
      "Total run time",
      PhaseUtils.prettyTime(phaseSummaryStats.totalDuration),
      "100.00%")
    out.println()
  }

  /**
   * Prints all statistics from [[PhaseStatistics]] in text form.
   */
  private def printPhaseStatistics(out: PrintStream, stats: PhaseStatistics): Unit = {
    out.println(s"=== ${stats.phase.name.toUpperCase} PHASE INFORMATION ===")

    printTwoColumns(out,
      s"Total ${stats.phase.name} phase time",
      PhaseUtils.prettyTime(stats.phaseDuration))
    out.println()

    if (stats.nonEmpty) {
      printTimingDistribution(out, stats)
      out.println()
    }
  }

  private def printExecutionPhaseStatistics(out: PrintStream): Unit = {
    val execPhase = phaseStatistics(ProfilePhase.Exec)
    val finishPhase = phaseStatistics(ProfilePhase.Finish)
    if (!execPhase.wasExecuted) {
      return
    }
    out.println("=== EXECUTION PHASE INFORMATION ===")

    if (execPhase.wasExecuted) {
      printTwoColumns(out,
        "Total execution phase time",
        PhaseUtils.prettyTime(execPhase.phaseDuration))
    }
    if (finishPhase.wasExecuted) {
      printTwoColumns(out,
        "Total time finalizing build",
        PhaseUtils.prettyTime(finishPhase.phaseDuration))
    }
    out.println()

    /*CriticalPathText criticalPaths = null;
    if (criticalPathStatistics.isPresent()) {
      criticalPaths = new CriticalPathText(out, criticalPathStatistics.get(), execTime);
      criticalPaths.printTimingBreakdown();
      printLn();
    }*/

    printTimingDistribution(out, execPhase)
    out.println()

    /*if (criticalPathStatistics.isPresent()) {
      criticalPaths.printCriticalPaths();
      printLn();
    }*/

    /*if (missingActionsCount > 0) {
      lnPrint(missingActionsCount);
      print(
        " action(s) are present in the"
            + " action graph but missing instrumentation data. Most likely the profile file"
            + " has been created during a failed or aborted build.");
      printLn();
    }*/
  }

  /**
   * Prints a table of task types and their relative total and average execution time as well as
   * how many tasks of each type there were
   */
  private def printTimingDistribution(out: PrintStream, stats: PhaseStatistics): Unit = {
    out.println("Total time (across all threads) spent on:")
    out.println("%18s %8s %8s %11s".format("Type", "Total", "Count", "Average"))
    stats.foreach { typ =>
      out.println("%18s %8s %8d %11s".format(
        typ.toString,
        PhaseUtils.prettyPercentage(stats.totalRelativeDuration(typ)),
        stats.getCount(typ),
        PhaseUtils.prettyTime(stats.meanDuration(typ))))
    }
  }

  private def printTwoColumns(out: PrintStream, col1: String, col2: String): Unit = {
    out.println("%-37s %10s".format(col1, col2))
  }

  private def printThreeColumns(out: PrintStream, col1: String, col2: String, col3: String): Unit = {
    out.println("%-28s %10s %8s".format(col1, col2, col3))
  }
}