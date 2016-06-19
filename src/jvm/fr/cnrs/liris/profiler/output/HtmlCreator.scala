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

import java.io.{BufferedOutputStream, FileOutputStream, PrintStream}
import java.nio.file.Path

import fr.cnrs.liris.profiler.chart.DetailedChartCreator
import fr.cnrs.liris.profiler.output.HtmlPrinter._
import fr.cnrs.liris.profiler.statistics.{PhaseStatistics, PhaseSummaryStatistics}
import fr.cnrs.liris.profiler.{ProfileInfo, ProfilePhase}

/**
 * Creates an HTML page displaying the various statistics and charts generated
 * from the profile file.
 */
class HtmlCreator(
  out: PrintStream,
  title: String,
  chart: Option[ChartHtml],
  /*multiFileStats: Option[MultiProfilePhaseHtml],*/
  phases: PhaseHtml) {

  /**
   * Output the HTML depending on which statistics should be printed.
   */
  def print(): Unit = {
    out.tag("html") {
      out.tag("head") {
        out.element("title", title)
        printVisualizationJs()
        chart.foreach(_.printCss(out))
        phases.printCss(out)
        /*if (multiFileStats.isPresent()) {
        multiFileStats.get().printHtmlHead();
        }*/
      }
      out.tag("body") {
        out.element("h1", title)
        chart.foreach(_.print(out))

        out.element("a", "name", "Statistics")
        out.element("h2", "Statistics")
        phases.print(out)

        /*if (multiFileStats.isPresent()) {
        multiFileStats.get().printHtmlBody();
        }*/
      }
    }
  }

  /**
   * Print code for loading the Google Visualization JS library.
   *
   * <p>Used for the charts and tables for [[MultiProfilePhaseHtml]].
   * Also adds a callback on load of the library which draws the charts and tables.
   */
  private def printVisualizationJs() = {
    out.element("script", "type", "text/javascript", "src", "https://www.google.com/jsapi")
    out.tag("script", "type", "text/javascript") {
      out.println("google.load(\"visualization\", \"1.1\", {packages:[\"corechart\",\"table\"]});")
      out.println("google.setOnLoadCallback(drawVisualization);")
      out.println("function drawVisualization() {")
      /*if (multiFileStats.isPresent()) {
      multiFileStats.get().printVisualizationCallbackJs();
    }*/
      out.println("}")
    }
  }


}

object HtmlCreator {
  /**
   * Writes the HTML profiling information.
   */
  def write(
    info: ProfileInfo,
    file: Path,
    /*CriticalPathStatistics criticalPathStats,
    missingActionsCount: Int,*/
    //detailed: Boolean,
    pixelsPerSecond: Int = ChartHtml.DefaultPixelsPerSecond,
    generateChart: Boolean = true,
    generateHistograms: Boolean = true): Unit = {
    val out = new PrintStream(new BufferedOutputStream(new FileOutputStream(file.toFile)))
    val phaseSummaryStats = PhaseSummaryStatistics(info)
    val phaseStats = ProfilePhase.values.map { phase => phase -> new PhaseStatistics(phase, info) }.toMap

    val phaseHtml = new PhaseHtml(phaseSummaryStats, phaseStats)
    val chartHtml = if (generateChart) {
      val chartCreator = new DetailedChartCreator(info)
      /*if (detailed) {
        chartCreator = new DetailedChartCreator(info);
      } else {
        chartCreator = new AggregatingChartCreator(info);
      }*/
      Some(new ChartHtml(chartCreator.create, pixelsPerSecond))
    } else {
      None
    }
    new HtmlCreator(out, info.comment.getOrElse(""), chartHtml, phaseHtml).print()
    out.close()
  }
}