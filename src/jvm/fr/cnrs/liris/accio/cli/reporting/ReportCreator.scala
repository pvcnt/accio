package fr.cnrs.liris.accio.cli.reporting

import java.io.{BufferedOutputStream, FileOutputStream, PrintStream}
import java.nio.file.Path

import fr.cnrs.liris.accio.core.thrift
import fr.cnrs.liris.common.util.FileUtils

/**
 * Create visual reports in various ways.
 */
trait ReportCreator {
  /**
   * Print a report about [[ReportStatistics]] to a stream.
   *
   * @param reportStats Statistics about serialized metrics
   * @param out         Output stream
   */
  def print(reportStats: ReportStatistics, out: PrintStream): Unit

  /**
   * Print a report about some serialized metrics to a stream.
   *
   * @param reports Serialized metrics
   * @param out     Output stream
   */
  def print(reports: Seq[thrift.Report], out: PrintStream): Unit =
    print(new ReportStatistics(reports), out)

  /**
   * Write a report about some serialized metrics to a file. The output file will be deleted before
   * if it already exists.
   *
   * @param reports Serialized metrics
   * @param path    Output file
   */
  def write(reports: Seq[thrift.Report], path: Path): Unit = {
    val reportStats = new ReportStatistics(reports)
    FileUtils.safeDelete(path)
    val out = new PrintStream(new BufferedOutputStream(new FileOutputStream(path.toFile)))
    try {
      print(reportStats, out)
    } finally {
      out.close()
    }
  }
}