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

import com.google.inject.Guice
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.core.framework.AccioFinatraJacksonModule
import fr.cnrs.liris.accio.ops.OpsModule
import fr.cnrs.liris.common.flags._

import scala.util.control.NonFatal

object AccioAppMain extends AccioApp

case class AccioAppLaunchFlags(
  @Flag(name = "cores", help = "Number of cores to use")
  cores: Int = 0)

class AccioApp extends StrictLogging {
  def main(args: Array[String]): Unit = {
    val reporter = new StreamReporter(Console.out, useColors = true)
    val injector = Guice.createInjector(AccioModule, OpsModule, AccioFinatraJacksonModule, FlagsModule)

    val registry = injector.getInstance(classOf[CommandRegistry])
    val name = args.headOption.getOrElse("help")
    val meta = registry.get(name) match {
      case None =>
        reporter.writeln(s"<error>Unknown command '$name'</error>")
        registry("help")
      case Some(m) => m
    }
    val parserFactory = injector.getInstance(classOf[FlagsParserFactory])
    val flags = parseFlags(parserFactory, meta, args.drop(1))
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

  private def parseFlags(parserFactory: FlagsParserFactory, meta: CommandMeta, args: Seq[String]): FlagsProvider = {
    val parser = parserFactory.create(meta.defn.allowResidue, meta.defn.flags: _*)
    parser.parseAndExitUponError(args)
    parser
  }
}