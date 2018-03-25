/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016-2018 Vincent Primault <v.primault@ucl.ac.uk>
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

package fr.cnrs.liris.accio.tools.cli.command

import java.nio.file.Files

import com.google.inject.Inject
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.agent.{AgentService$FinagleClient, CreateRunRequest, ParseRunRequest}
import fr.cnrs.liris.accio.api.Utils
import fr.cnrs.liris.accio.api.thrift.RunSpec
import fr.cnrs.liris.accio.runtime.cli.{Cmd, ExitCode}
import fr.cnrs.liris.accio.runtime.event.{Event, EventKind, Reporter}
import fr.cnrs.liris.common.flags.{Flag, FlagsProvider}
import fr.cnrs.liris.common.util.{FileUtils, StringUtils}

import scala.collection.JavaConverters._

case class SubmitCommandFlags(
  @Flag(
    name = "name",
    help = "Run name")
  name: Option[String],
  @Flag(
    name = "tags",
    help = "Run tags (comma-separated)")
  tags: Option[String],
  @Flag(
    name = "notes",
    help = "Run notes")
  notes: Option[String],
  @Flag(
    name = "repeat",
    help = "Number of times to repeat each run")
  repeat: Option[Int],
  @Flag(
    name = "seed",
    help = "Seed to use for unstable operators")
  seed: Option[Long])

@Cmd(
  name = "submit",
  flags = Array(classOf[SubmitCommandFlags], classOf[ClusterFlags]),
  help = "Launch an Accio workflow.",
  allowResidue = true)
class SubmitCommand @Inject()(clientProvider: ClusterClientProvider)
  extends ClientCommand(clientProvider) with StrictLogging {

  def execute(flags: FlagsProvider, out: Reporter): ExitCode = {
    if (flags.residue.isEmpty) {
      out.handle(Event.error("You must provide a run file or package specification."))
      ExitCode.CommandLineError
    } else {
      val params = try {
        parseParams(flags.residue.tail)
      } catch {
        case e: IllegalArgumentException =>
          out.handle(Event.error(s"Params argument parse error: ${e.getMessage}"))
          return ExitCode.CommandLineError
      }
      val client = createClient(flags)
      val opts = flags.as[SubmitCommandFlags]
      parseAndSubmit(flags.residue.head, params, opts, client, out)
    }
  }

  private def parseAndSubmit(uri: String, params: Map[String, String], opts: SubmitCommandFlags, client: AgentService$FinagleClient, out: Reporter): ExitCode = {
    val path = FileUtils.expandPath(uri)
    val file = path.toFile
    val content = if (file.exists) {
      if (!file.canRead) {
        out.handle(Event.error(s"Cannot read workflow definition file: ${path.toAbsolutePath}"))
        return ExitCode.DefinitionError
      }
      Files.readAllLines(path).asScala.mkString
    } else {
      uri
    }

    val req = ParseRunRequest(content, params, Some(path.getFileName.toString))
    handleResponse(client.parseRun(req), out) { resp =>
      printErrors(resp.warnings, out, EventKind.Warning)
      printErrors(resp.errors, out, EventKind.Error)
      resp.run match {
        case Some(spec) =>
          val mergedSpec = merge(spec, opts)
          submit(mergedSpec, opts, client, out)
        case None =>
          out.handle(Event.error("Some errors where found in the run definition"))
          ExitCode.DefinitionError
      }
    }
  }

  private def submit(spec: RunSpec, opts: SubmitCommandFlags, client: AgentService$FinagleClient, out: Reporter) = {
    val req = CreateRunRequest(spec, Utils.DefaultUser)
    handleResponse(client.createRun(req), out) { resp =>
      resp.ids.foreach {
        runId =>
          out.handle(Event.info(s"Created run ${runId.value}"))
      }
      if (resp.ids.size > 1) {
        out.handle(Event.info(s"Created ${resp.ids.size} runs successfully"))
      }
      ExitCode.Success
    }
  }

  private[this] val ParamRegex = "([^=]+)=(.+)".r

  private def parseParams(params: Seq[String]): Map[String, String] = {
    params.map {
      case ParamRegex(paramName, value) => paramName -> value
      case str => throw new IllegalArgumentException(s"Invalid param (expected key=value): $str")
    }.toMap
  }

  private def merge(spec: RunSpec, opts: SubmitCommandFlags) = {
    var newSpec = spec
    opts.name.foreach {
      name =>
        newSpec = newSpec.copy(name = Some(name))
    }
    opts.notes.foreach {
      notes =>
        newSpec = newSpec.copy(notes = Some(notes))
    }
    val tags = StringUtils.explode(opts.tags, ",")
    if (tags.nonEmpty) {
      newSpec = newSpec.copy(tags = newSpec.tags ++ tags)
    }
    opts.repeat.foreach {
      repeat =>
        newSpec = newSpec.copy(repeat = Some(repeat))
    }
    opts.seed.foreach {
      seed =>
        newSpec = newSpec.copy(seed = Some(seed))
    }
    newSpec
  }
}