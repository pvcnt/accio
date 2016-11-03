/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016 Vincent Primault <vincent.primault@liris.cnrs.fr>
 *
 * Accio is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Accio is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Accio.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.cnrs.liris.accio.cli

import java.io.PrintStream
import java.util.concurrent.atomic.AtomicInteger

import fr.cnrs.liris.common.util.HtmlPrinter._
import fr.cnrs.liris.accio.core.framework._
import fr.cnrs.liris.common.stats.{AggregatedStats, Distribution}
import fr.cnrs.liris.common.util.MathUtils.roundAt4

/**
 * Report about serialized events in a human-readable HTML format.
 */
class HtmlReportCreator(showArtifacts: Set[String] = Set.empty) {
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
      if (showArtifacts.nonEmpty) {
        out.tag("li", "class", "active") {
          out.element("a", "href", "#tab-evals", "data-toggle", "tab", "Artifacts")
        }
      }
    }
  }

  private def printContent(out: PrintStream, reportStats: ReportStatistics) = {
    out.tag("div", "class", "tab-content") {
      if (showArtifacts.nonEmpty) {
        out.tag("div", "class", "tab-pane active", "id", "tab-evals") {
          reportStats
            .artifacts
            .filter { case (name, _) => showArtifacts.contains(name) }
            .foreach { case (name, artifacts) => printArtifacts(out, name, artifacts) }
        }
      }
    }
  }

  private def printArtifacts(out: PrintStream, name: String, artifacts: Map[String, Artifact]) = {
    if (artifacts.nonEmpty) {
      artifacts.head._2.kind match {
        case DataType.List(DataType.Double) =>
          printDistributionArtifacts(out, name, normalizeArtifacts[Seq[Double]](artifacts))
        case DataType.Map(_, DataType.Double) =>
          printDistributionArtifacts(out, name, normalizeArtifacts[Map[Any, Double]](artifacts).mapValues(_.values.toSeq))
        case DataType.List(DataType.Long) =>
          printDistributionArtifacts(out, name, normalizeArtifacts[Seq[Long]](artifacts).mapValues(_.map(_.toDouble)))
        case DataType.Map(_, DataType.Long) =>
          printDistributionArtifacts(out, name, normalizeArtifacts[Map[Any, Long]](artifacts).mapValues(_.values.map(_.toDouble).toSeq))
        case _ => // Nothing to display.
      }
    }
  }

  private def normalizeArtifacts[T](artifacts: Map[String, Artifact]): Map[String, T] = {
    val multiplePrefixes = artifacts.keySet.map(_.split("/").head).size > 1
    artifacts.map { case (name, art) =>
      val key = if (multiplePrefixes) name else name.split("/").tail.mkString("/")
      key -> art.value.asInstanceOf[T]
    }
  }

  private def printDistributionArtifacts(out: PrintStream, title: String, artifacts: Map[String, Seq[Double]]) = {
    val distributions = artifacts.map { case (name, values) => name -> Distribution(values) }
    printDistributions(out, title, distributions)
    artifacts.foreach { case (name, values) =>
      printStats(out, AggregatedStats(values), Some(name))
    }
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
        out.println(s" labels:[${seq.map(_._1).map(quote).mkString(",")}],")
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