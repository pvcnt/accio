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

package fr.cnrs.liris.accio.client

import java.nio.file.Paths

import com.google.inject.Inject
import com.twitter.util.{Await, Stopwatch}
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.agent.AgentService
import fr.cnrs.liris.accio.client.parser._
import fr.cnrs.liris.accio.core.domain.Utils
import fr.cnrs.liris.accio.core.service.handler.CreateRunRequest
import fr.cnrs.liris.common.flags.{Flag, FlagsProvider}
import fr.cnrs.liris.common.util.{StringUtils, TimeUtils}

import scala.collection.mutable

case class RunFlags(
  @Flag(name = "name", help = "Run name")
  name: Option[String],
  @Flag(name = "environment", help = "Environment")
  environment: Option[String],
  @Flag(name = "tags", help = "Space-separated run tags")
  tags: Option[String],
  @Flag(name = "notes", help = "Run notes")
  notes: Option[String],
  @Flag(name = "repeat", help = "Number of times to repeat each run")
  repeat: Option[Int],
  @Flag(name = "params", help = "Parameters")
  params: Option[String],
  @Flag(name = "seed", help = "Seed to use for unstable operators")
  seed: Option[Long])

@Cmd(
  name = "run",
  flags = Array(classOf[RunFlags]),
  help = "Execute an Accio workflow.",
  allowResidue = true)
class RunCommand @Inject()(agentClient: AgentService.FinagledClient, parser: RunTemplateParser, factory: RunTemplateFactory)
  extends Command with StrictLogging {

  private[this] val ParamRegex = "([^=]+)=(.+)".r

  def execute(flags: FlagsProvider, out: Reporter): ExitCode = {
    if (flags.residue.size != 1) {
      out.writeln("<error>You must provide exactly one run file.</error>")
      ExitCode.CommandLineError
    } else {
      val opts = flags.as[RunFlags]
      val elapsed = Stopwatch.start()

      val partials = mutable.ListBuffer.empty[ClientRunTemplate]
      val path = Paths.get(flags.residue.head)
      var pkg: Option[String] = None
      if (path.toFile.exists) {
        partials += parser.parse(path)
      } else {
        pkg = Some(flags.residue.head)
      }
      partials += ClientRunTemplate(
        pkg,
        opts.environment,
        opts.name,
        opts.notes,
        StringUtils.explode(opts.tags, ""),
        opts.seed,
        parseParams(opts.params),
        opts.repeat)

      val template = factory.create(partials: _*)
      println(template)

      val req = CreateRunRequest(template, Utils.DefaultUser)
      val resp = Await.result(agentClient.createRun(req))

      out.writeln(s"Created ${resp.ids.size} runs: ${resp.ids.map(_.value).mkString(", ")}")

      out.writeln(s"Done in ${TimeUtils.prettyTime(elapsed())}.")
      ExitCode.Success
    }
  }

  private def parseParams(params: Option[String]): Map[String, Exploration] = {
    params match {
      case Some(p) => p.trim.split(" ").map {
        case ParamRegex(paramName, value) => paramName -> SingletonExploration(value)
        case str => throw new IllegalArgumentException(s"Invalid param (expected key=value): $str")
      }.toMap
      case None => Map.empty
    }
  }
}