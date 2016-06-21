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

import java.nio.file.Paths

import com.google.inject.Inject
import fr.cnrs.liris.accio.cli.{Command, Reporter}
import fr.cnrs.liris.accio.core.pipeline.ExperimentReader
import fr.cnrs.liris.common.flags.{Flag, FlagsProvider}
import fr.cnrs.liris.common.util.FileUtils

case class SummarizeCommandOpts(
    @Flag(name = "html")
    html: Boolean = false,
    @Flag(name = "gnuplot")
    gnuplot: Boolean = false,
    @Flag(name = "what")
    gnuplotWhat: String = ""
)

@Command(
  name = "summarize",
  help = "Generate summaries from previously generated reports",
  allowResidue = true)
class SummarizeCommand @Inject()(reader: ExperimentReader) extends AccioCommand[SummarizeCommandOpts] {

  def execute(flags: FlagsProvider, out: Reporter): ExitCode = {
    val opts = flags.as[SummarizeCommandOpts]
    val path = Paths.get(FileUtils.replaceHome(flags.residue.head))
    //val experiment = reader.readExperiment(path)

    if (opts.html) {
      val htmlFile = path.resolveSibling(path.getFileName.toString + ".html")
      //val summarizer = new HtmlReportCreator
      //summarizer.write(eventReader.reports, htmlFile)
    } else if (opts.gnuplot) {
      require(opts.gnuplotWhat.nonEmpty)
      require(opts.gnuplotWhat.contains(":"))
      val parts = opts.gnuplotWhat.split(":")
      val kind = parts.head
      val name = parts.tail.mkString(":")
      val file = path.resolveSibling(path.getFileName.toString + "-" + name.replace("/", "__") + ".plt")
      //val summarizer = new GnuplotReportCreator(name, kind)
      //summarizer.write(eventReader.reports, file)
      /*val exitCode = s"gnuplot ${file.toAbsolutePath}".!
      if (exitCode != 0) {
        println("Failed to execute gnuplot")
      }*/
    } else {
      //val summarizer = new TextReportCreator
      //summarizer.print(eventReader.reports, Console.out)
    }
    ExitCode.Success
  }
}