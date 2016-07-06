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

package fr.cnrs.liris.accio.cli

import java.io.PrintStream
import java.util.concurrent.atomic.AtomicInteger

import fr.cnrs.liris.accio.cli.HtmlPrinter._
import fr.cnrs.liris.accio.core.pipeline._
import fr.cnrs.liris.common.stats.{AggregatedStats, Distribution}
import fr.cnrs.liris.common.util.MathUtils.roundAt4

/**
 * Report about serialized events in a human-readable HTML format.
 */
class HtmlReportCreator(showArtifacts: Set[String] = Set.empty, showParameters: Set[String] = Set.empty, showGraph: Boolean = false) {
  private[this] val idGenerator = new AtomicInteger(0)
  private[this] val cdfNbSteps = 100

  def print(reportStats: ReportStatistics, out: PrintStream): Unit = {
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
      if (showGraph) {
        out.tag("li") {
          out.element("a", "href", "#tab-graph", "data-toggle", "tab", "Execution graph")
        }
      }
      if (showArtifacts.nonEmpty) {
        out.tag("li", "class", "active") {
          out.element("a", "href", "#tab-evals", "data-toggle", "tab", "Artifacts")
        }
      }
      if (showParameters.nonEmpty) {
        out.tag("li") {
          out.element("a", "href", "#tab-params", "data-toggle", "tab", "Parameters")
        }
      }
    }
  }

  private def printContent(out: PrintStream, reportStats: ReportStatistics) = {
    out.tag("div", "class", "tab-content") {
      if (showGraph) {
        out.tag("div", "class", "tab-pane active", "id", "tab-graph") {
          reportStats.runs.head.graphDef
        }
      }
      if (showArtifacts.nonEmpty) {
        out.tag("div", "class", "tab-pane active", "id", "tab-evals") {
          reportStats
              .artifacts
              .filter { case (name, _) => showArtifacts.contains(name) }
              .foreach { case (name, artifacts) => printArtifacts(out, name, artifacts) }
        }
        if (showParameters.nonEmpty) {
          out.tag("div", "class", "tab-pane", "id", "tab-params") {
            out.tag("div", "class", "row") {
              reportStats.similarGraphs.filter(_.size > 1).foreach(printParameters(out, _))
              //reportStats.reports.filter(_.isOptimization).foreach(report => printSolutionChart(out, report))
            }
          }
        }
      }
    }
  }

  private def printArtifacts(out: PrintStream, name: String, artifacts: Map[String, Artifact]) = {
    if (artifacts.nonEmpty) {
      artifacts.head._2 match {
        case _: ScalarArtifact =>
          require(artifacts.values.forall(_.isInstanceOf[ScalarArtifact]))
        case _: DistributionArtifact =>
          require(artifacts.values.forall(_.isInstanceOf[DistributionArtifact]))
          printDistributionArtifacts(out, name, normalizeArtifacts[DistributionArtifact](artifacts))
        case _: StoredDatasetArtifact =>
          require(artifacts.values.forall(_.isInstanceOf[StoredDatasetArtifact]))
      }
    }
  }

  private def normalizeArtifacts[T <: Artifact](artifacts: Map[String, Any]): Map[String, T] = {
    val multiplePrefixes = artifacts.keySet.map(_.split("/").head).size > 1
    artifacts.map { case (name, art) =>
      val key = if (multiplePrefixes) name else name.split("/").tail.mkString("/")
      key -> art
    }.asInstanceOf[Map[String, T]]
  }

  private def printDistributionArtifacts(out: PrintStream, title: String, artifacts: Map[String, DistributionArtifact]) = {
    val distributions = artifacts.map { case (name, art) => name -> Distribution(art.values) }
    printDistributions(out, title, distributions)
    artifacts.foreach { case (name, dist) =>
      printStats(out, AggregatedStats(dist.values), Some(name))
    }
  }

  private def printParameters(out: PrintStream, graphs: Seq[GraphDef]) = {
    val params = graphs
        .flatMap(_.nodes)
        .flatMap(node => node.paramMap.toSeq.map { case (name, value) => s"${node.name}/$name" -> value })
        .groupBy(_._1)
        .map { case (name, values) => name -> values.map(_._2) }
  }

  private def printCategoricalParameter(out: PrintStream, title: String, dist: Distribution[String]): Unit = {
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
        out.println(s"Plotly.newPlot('$id', data, {title:${quote(title)}}, {showLink:false, displaylogo:false});")
        out.println("})();")
      }
      //}
    }
  }

  private def printScalarParameter(out: PrintStream, title: String, distByUser: Map[String, Distribution[Double]]) = {
    val statsByUser = distByUser.values.map(_.toStats).toSeq
    val id = title.replace("/", "__")
    out.tag("div", "class", "col-sm-6", s"div-$id") {
      val stddevSeries = statsByUser.filter(_.n > 1).map(_.stddev)
      val rangeSeries = statsByUser.filter(_.n > 1).map(_.range)

      if (stddevSeries.nonEmpty) {
        val min = math.min(stddevSeries.min, rangeSeries.min)
        val max = math.max(stddevSeries.max, rangeSeries.max)
        val stddevCdf = Distribution(stddevSeries).cdf(min, max, cdfNbSteps)
        val rangeCdf = Distribution(rangeSeries).cdf(min, max, cdfNbSteps)
        printScatter(out, s"chart-$id", s"$title variability", Map("stddev" -> stddevCdf, "range" -> rangeCdf))

        printStats(out, AggregatedStats(stddevSeries), Some("stddev"))
        printStats(out, AggregatedStats(rangeSeries), Some("range"))
      }
    }
  }

  private def printDistributions(out: PrintStream, title: String, dists: Map[String, Distribution[Double]]) = {
    val nonEmptyDists = dists.filter(_._2.nonEmpty)
    if (nonEmptyDists.nonEmpty) {
      val id = title.replace("/", "__")
      val min = nonEmptyDists.values.map(_.min).min
      val max = nonEmptyDists.values.map(_.max).max
      val cdfs = nonEmptyDists.map { case (name, dist) => name -> dist.cdf(min, max, cdfNbSteps) }
      printScatter(out, s"chart-$id", title, cdfs)
    }
  }

  private def printScatter(out: PrintStream, id: String, title: String, cdfs: Map[String, Seq[(Double, Double)]], lines: Boolean = true) = {
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
      /*if (moveLegend) {
        out.print(",legend:{y:0.1,x:0.25}")
      }*/
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