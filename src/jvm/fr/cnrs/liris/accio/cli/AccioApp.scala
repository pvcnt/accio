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

import com.google.inject.Guice
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.common.flags.{Flag, FlagsData, FlagsParser, FlagsProvider}
import fr.cnrs.liris.accio.cli.commands._

import scala.reflect.runtime.universe._
import scala.util.control.NonFatal

object AccioAppMain extends AccioApp

case class AccioAppLaunchFlags(
    @Flag(name = "cores", help = "Number of cores to use")
    cores: Int = 0)

class AccioApp extends StrictLogging {
  def main(args: Array[String]): Unit = {
    val reporter = new StreamReporter(Console.out, useColors = true)
    val injector = Guice.createInjector(new AccioModule)

    val registry = injector.getInstance(classOf[CommandRegistry])
    val name = args.headOption.getOrElse("help")
    val meta = registry.get(name) match {
      case None =>
        reporter.writeln(s"<error>Unknown command '$name'</error>")
        registry("help")
      case Some(m) => m
    }
    val flags = parseFlags(meta, args.drop(1))
    val launchFlags = flags.as[AccioAppLaunchFlags]
    val command = injector.getInstance(meta.clazz)

    val exitCode = try {
      command.execute(flags, reporter)
    } catch {
      case e: IllegalArgumentException =>
        reporter.writeln(s"<error>${e.getMessage.stripPrefix("requirement failed:").trim}</error>")
        ExitCode.RuntimeError
      case e: RuntimeException =>
        reporter.writeln(s"<error>${e.getMessage}</error>")
        ExitCode.RuntimeError
      case NonFatal(e) =>
        reporter.writeln(s"<error>${e.getMessage}</error>")
        e.getStackTrace.foreach(elem => reporter.writeln(s"  <error>$elem</error>"))
        ExitCode.InternalError
    }

    sys.exit(exitCode.code)
  }

  private def parseFlags(meta: CommandMeta, args: Seq[String]): FlagsProvider = {
    val flagsData = FlagsData.of(meta.flagsTypes ++ Seq(typeOf[AccioAppLaunchFlags]))
    val parser = new FlagsParser(flagsData, allowResidue = true)
    parser.parseAndExitUponError(args)
    parser
  }
}