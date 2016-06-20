package fr.cnrs.liris.accio.cli.reporting

import java.io.PrintStream

import fr.cnrs.liris.common.stats.{AggregatedStats, Distribution}
import fr.cnrs.liris.common.util.MathUtils.roundAt6

/**
 * Report about serialized events in a human-readable text format.
 */
class TextReportCreator(showMetrics: Boolean = true, showSolutions: Boolean = true, useAnsi: Boolean = true) extends ReportCreator {
  override def print(reportStats: ReportStatistics, out: PrintStream): Unit = {
    reportStats.reports.foreach { report =>
      out.println(s"=== $report ===")
      if (showMetrics) {
        printMetrics(out, report)
      }
      if (showSolutions && report.isOptimization) {
        printSolutions(out, report)
      }
    }
  }

  private def printMetrics(out: PrintStream, report: ReportStatistics#Report) = {
    out.print(s"${" " * 14}${bold("metric")} | ${" " * 5}${bold("count")} | ${" " * 7}${bold("min")} | ")
    out.println(f"${" " * 7}${bold("max")} | ${" " * 6}${bold("mean")} | ${" " * 4}${bold("stddev")}")
    report.evaluations.foreach { metric =>
      report.stats(metric).foreach { stats =>
        printSummary(out, metric, stats)
      }
    }
    out.println()
  }

  private def printSolutions(out: PrintStream, report: ReportStatistics#Report) = {
    out.println(s"${" " * 15}${bold("param")} | ${bold("distribution")}")
    report.params.foreach { param =>
      out.print(f"${param.name}%20s | ")
      param match {
        case p: DoubleParam => printDistribution(out, report.distribution(p), bucketsCount = 10)
        case p: StringParam => printDistribution(out, report.distribution(p))
      }
    }
    out.println()
  }

  private def printDistribution(out: PrintStream, dist: Distribution[String]): Unit = {
    if (dist.isSingleValue) {
      out.println(s"${dist.distinctValues.head}")
    } else {
      dist.distinctValues.zipWithIndex.foreach { case (value, idx) =>
        if (idx > 0) {
          out.print(s"${" " * 20} |")
        }
        out.println(s"$value: ${dist.count(value)}")
      }
    }
  }

  private def printDistribution(out: PrintStream, dist: Distribution[Double], bucketsCount: Int): Unit = {
    if (dist.isSingleValue) {
      out.println(s"${dist.distinctValues.head}")
    } else {
      dist.buckets(bucketsCount).zipWithIndex.foreach { case ((until, count), idx) =>
        if (idx > 0) {
          out.print(s"${" " * 20} | ")
        }
        out.println(s"<= ${roundAt6(until)}: $count")
      }
    }
  }

  /**
   * Print statistical summary for a given metric. Depending on the actual number of values, it
   * will either print descriptive statistics or the values (if there are 2 or less values).
   *
   * @param out    Output stream
   * @param metric Metric name
   * @param stats  Serialized statistics for this metric
   */
  private def printSummary(out: PrintStream, metric: String, stats: AggregatedStats) = {
    out.print(f"$metric%20s | ${stats.n}%10s")
    if (stats.n > 0) {
      out.print(f" | ${roundAt6(stats.min)}%10s")
      if (stats.n > 1) {
        out.print(f" | ${roundAt6(stats.max)}%10s | ${roundAt6(stats.avg)}%10s | ${roundAt6(stats.stddev)}%10s")
      }
    }
    out.println()
  }

  private def bold(str: String) = if (useAnsi) s"\033[1m$str\033[0m" else str
}