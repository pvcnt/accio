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

package fr.cnrs.liris.accio.cli.reporting

import java.io.PrintStream
import java.util.concurrent.atomic.AtomicInteger

import fr.cnrs.liris.common.stats.{AggregatedStats, Distribution}
import fr.cnrs.liris.common.util.MathUtils.roundAt4
import fr.cnrs.liris.profiler.output.HtmlPrinter._

/**
 * Report about serialized events in a human-readable HTML format.
 */
class HtmlReportCreator(
    showMetrics: Boolean = true,
    showSolutions: Boolean = true,
    showInsights: Boolean = true
) extends ReportCreator {
  private[this] val idGenerator = new AtomicInteger(0)
  private[this] val cdfNbSteps = 100

  override def print(reportStats: ReportStatistics, out: PrintStream): Unit = {
    out.tag("html") {
      out.tag("head") {
        out.element("meta", "charset", "utf-8")
        out.element("link", "rel", "stylesheet", "href", "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css")
        out.element("script", "src", "https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js")
        out.element("script", "src", "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/js/bootstrap.min.js")
        out.element("script", "src", "https://cdn.plot.ly/plotly-latest.min.js", "charset", "utf-8")
      }
      out.tag("body") {
        out.tag("div", "class", "container") {
          printNav(out)
          printContent(out, reportStats)
        }
      }
    }
  }

  private def printNav(out: PrintStream) = {
    out.tag("ul", "class", "nav nav-tabs") {
      if (showMetrics) {
        out.tag("li", "class", "active") {
          out.element("a", "href", "#tab-evals", "data-toggle", "tab", "Evaluations")
        }
      }
      if (showInsights) {
        out.tag("li") {
          out.element("a", "href", "#tab-analyses", "data-toggle", "tab", "Analyses")
        }
      }
      if (showSolutions) {
        out.tag("li") {
          out.element("a", "href", "#tab-params", "data-toggle", "tab", "Parameters")
        }
      }
    }
  }

  private def printContent(out: PrintStream, reportStats: ReportStatistics) = {
    out.tag("div", "class", "tab-content") {
      if (showMetrics) {
        out.tag("div", "class", "tab-pane active", "id", "tab-evals") {
          //out.tag("div", "class", "row") {
          reportStats.evaluations.foreach(metric => printMetricChart(out, reportStats, metric))
          //}
          /*val cdfs = reportStats.reports.map { report =>
            val cdf = report.correlation("privacy/fscore", "utility/avg")
            report.toString -> cdf
          }.toMap
          printScatter(out, "correlation", cdfs, lines = false)*/
        }
      }
      if (showInsights) {
        out.tag("div", "class", "tab-pane", "id", "tab-analyses") {
          out.tag("div", "class", "row") {
            reportStats.analyses.foreach(metric => printAnalysisChart(out, reportStats, metric))
          }
        }
      }
      if (showSolutions) {
        out.tag("div", "class", "tab-pane", "id", "tab-params") {
          out.tag("div", "class", "row") {
            reportStats.reports.filter(_.isOptimization).foreach(report => printSolutionChart(out, report))
          }
          /*val cdfs = reportStats.reports.map { report =>
            val cdf = report.correlation2("epsilon", "pois/count")
            report.toString -> cdf
          }.toMap
          printScatter(out, "correlation", cdfs, lines = false)*/
        }
      }
    }
  }

  private def printSolutionChart(out: PrintStream, report: ReportStatistics#Report) = {
    report.params.foreach(printParamChart(out, report, _))
  }

  private def printParamChart(out: PrintStream, report: ReportStatistics#Report, param: Param): Unit = {
    param match {
      case p: StringParam => printParamChart(out, p, report.distribution(p))
      case p: DoubleParam =>
        printParamChart(out, p, report.distribution(p))
        printParamVariability(out, p, report.distributionByUser(p))
    }
  }

  private def printParamChart(out: PrintStream, param: DoubleParam, dist: Distribution[Double]): Unit = {
    val stats = AggregatedStats(dist)
    //out.tag("div", "class", "col-sm-6") {
    printScatter(out, param.name, Map(param.name -> dist.cdf(cdfNbSteps)))
    printStats(out, stats)
    //}
  }

  private def printParamChart(out: PrintStream, param: StringParam, dist: Distribution[String]): Unit = {
    if (dist.size > 1) {
      val id = s"chart-${idGenerator.incrementAndGet()}"
      //out.tag("div", "class", "col-sm-6") {
      out.element("div", "id", id)
      out.tag("script") {
        val seq = dist.toSeq
        out.println("(function() {")
        out.println("var data = [{")
        out.println(s" labels:[${seq.map(_._1).map(quote(_)).mkString(",")}],")
        out.println(s" values:[${seq.map(_._2).mkString(",")}],")
        out.println(" type:'pie'")
        out.println("}];")
        out.println(s"Plotly.newPlot('$id', data, {title:${quote(param.name)}}, {showLink:false, displaylogo:false});")
        out.println("})();")
      }
      //}
    }
  }

  private def printParamVariability(out: PrintStream, param: DoubleParam, distByUser: Map[String, Distribution[Double]]) = {
    val statsByUser = distByUser.values.map(_.toStats).toSeq
    out.tag("div", "class", "col-sm-6") {
      val stddevSeries = statsByUser.filter(_.n > 1).map(_.stddev)
      val rangeSeries = statsByUser.filter(_.n > 1).map(_.range)

      if (stddevSeries.nonEmpty) {
        val min = math.min(stddevSeries.min, rangeSeries.min)
        val max = math.max(stddevSeries.max, rangeSeries.max)
        val stddevCdf = Distribution(stddevSeries).cdf(min, max, cdfNbSteps)
        val rangeCdf = Distribution(rangeSeries).cdf(min, max, cdfNbSteps)
        printScatter(out, s"${param.name} variability", Map("stddev" -> stddevCdf, "range" -> rangeCdf))

        printStats(out, AggregatedStats(stddevSeries), Some("stddev"))
        printStats(out, AggregatedStats(rangeSeries), Some("range"))
      }
    }
  }

  private def printMetricChart(out: PrintStream, reportStats: ReportStatistics, metric: String) = {
    val distributions = reportStats.reports
        .map(report => report.toString -> report.evalDistribution(metric))
        .toMap
    printDists(out, metric, distributions)
    reportStats.reports.foreach { report =>
      printStats(out, AggregatedStats(report.evalDistribution(metric)), Some(report.toString))
    }
  }

  private def printAnalysisChart(out: PrintStream, reportStats: ReportStatistics, metric: String) = {
    val distributions = reportStats.reports
        .map(report => report.toString -> report.analysisDistribution(metric))
        .toMap
    printDists(out, metric, distributions)
  }

  private def printDists(out: PrintStream, title: String, dists: Map[String, Distribution[Double]], moveLegend: Boolean = false) = {
    val nonEmptyDists = dists.filter(_._2.nonEmpty)
    if (nonEmptyDists.nonEmpty) {
      //out.tag("div", "class", "col-sm-6") {
      val min = nonEmptyDists.values.map(_.min).min
      val max = nonEmptyDists.values.map(_.max).max
      //val max = percentile(nonEmptyDists.values.flatMap(_.values), .99)
      val cdfs = nonEmptyDists.map { case (name, dist) => name -> dist.cdf(min, max, cdfNbSteps) }
      printScatter(out, title, cdfs, moveLegend)
      //}
    }
  }

  private def printScatter(out: PrintStream, title: String, cdfs: Map[String, Seq[(Double, Double)]], moveLegend: Boolean = false, lines: Boolean = true) = {
    val id = s"chart-${idGenerator.incrementAndGet()}"
    out.element("div", "id", id)
    out.tag("script") {
      out.println("(function() {")
      out.println("var data = [")
      cdfs.foreach { case (name, cdf) =>
        out.println("{")
        out.println(s" x:[${cdf.map(v => roundAt4(v._1)).mkString(",")}],")
        out.println(s" y:[${cdf.map(v => roundAt4(v._2)).mkString(",")}],")
        out.println(s" mode:'${if (lines) "lines" else "markers"}',")
        out.println(" type:'scatter',")
        out.println(s" name:'$name'")
        out.println("},")
      }
      out.println("];")
      out.print(s"Plotly.newPlot('$id',data,")
      out.print(s"{margin:{b:60,t:50,l:40,r:40},title:'$title'")
      if (moveLegend) {
        out.print(",legend:{y:0.1,x:0.25}")
      }
      out.print("},{showLink:false, displaylogo:false}")
      out.println(");")
      out.println("})();")
    }
  }

  private def printStats(out: PrintStream, stats: AggregatedStats, label: Option[String] = None) = {
    out.tag("div", "style", "margin-bottom: 5px; margin-top: 5px; text-align: center;") {
      label.foreach { label =>
        out.element("span", "class", "label label-primary", "style", "margin-right: 5px", label)
      }
      out.element("span", "class", "label label-default", "min")
      out.print(s" ${roundAt4(stats.min)} ")
      out.element("span", "class", "label label-default", "max")
      out.print(s" ${roundAt4(stats.max)} ")
      out.element("span", "class", "label label-default", "n")
      out.print(s" ${roundAt4(stats.n)} ")
      out.element("span", "class", "label label-default", "avg")
      out.print(s" ${roundAt4(stats.avg)} ")
      out.element("span", "class", "label label-default", "stddev")
      out.print(s" ${roundAt4(stats.stddev)}")
    }
  }

  private def quote(str: String) = s"'$str'"
}