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

import fr.cnrs.liris.common.stats.Distribution
import breeze.stats.DescriptiveStats._

class GnuplotReportCreator(name: String, kind: String) extends ReportCreator {
  private[this] val cdfNbSteps = 100

  override def print(reportStats: ReportStatistics, out: PrintStream): Unit = {
    out.println("reset")
    out.println("set term epslatex monochrome")
    out.println(s"""set output "${name.replace("/", "__")}.eps"""")
    out.println("set grid")
    out.println("set size 0.75,0.7")
    out.println("set key right bottom")
    out.println(s"""set xlabel "$name"""")
    out.println("""set ylabel "Proportion of traces"""")
    val plots = reportStats.reports.map(report => s"""'-' title "${report.toString}" with lines""")
    out.println(s"plot ${plots.mkString(",")}")

    kind match {
      case "metric" => printDists(reportStats.reports.map(_.evalDistribution(name)), out)
      case "param" =>
        val dists = reportStats.reports.filter(_.isOptimization).flatMap { report =>
          val param = report.params.find(_.name == name)
          param.map(_.asInstanceOf[DoubleParam]).map(report.distribution)
        }
        printDists(dists, out)
      case "param-range" =>
        val dists = reportStats.reports.filter(_.isOptimization).flatMap { report =>
          val param = report.params.find(_.name == name)
          val series = param.map(_.asInstanceOf[DoubleParam]).map(report.distributionByUser(_).toSeq.map(_._2).map(_.toStats).filter(_.n > 1).map(_.range))
          series.map(Distribution(_))
        }
        printDists(dists, out)
      case "param-stddev" =>
        val dists = reportStats.reports.filter(_.isOptimization).flatMap { report =>
          val param = report.params.find(_.name == name)
          val series = param.map(_.asInstanceOf[DoubleParam]).map(report.distributionByUser(_).toSeq.map(_._2).map(_.toStats).filter(_.n > 1).map(_.stddev))
          series.map(Distribution(_))
        }
        printDists(dists, out)
      case "correlate" =>
        val parts = name.split("->")
        reportStats.reports.foreach { report =>
          val xs = report.evalDistribution(parts.head).values
          val ys = report.evalDistribution(parts.last).values
          xs.zip(ys).foreach { case (x, y) =>
              out.println(s"$x $y")
          }
          out.println("e")
        }
    }
  }

  private def printDists(dists: Seq[Distribution[Double]], out: PrintStream) = {
    val min = dists.map(_.min).min
    //val max = dists.map(_.max).max
    val max = percentile(dists.flatMap(_.values), .95)
    dists.foreach { dist =>
      val cdf = dist.cdf(min, max, cdfNbSteps)
      cdf.foreach { case (x, y) => out.println(s"$x $y") }
      out.println("e")
    }
  }
}
