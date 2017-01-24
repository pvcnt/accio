/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016-2017 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.accio.client.command

import com.google.inject.Inject
import com.twitter.util.{Await, Return, Stopwatch, Throw}
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.client.service._
import fr.cnrs.liris.accio.core.domain.{InvalidRunDefException, Utils}
import fr.cnrs.liris.accio.core.infra.cli.{Cmd, Command, ExitCode, Reporter}
import fr.cnrs.liris.accio.core.service.handler.CreateRunRequest
import fr.cnrs.liris.common.flags.{Flag, FlagsProvider}
import fr.cnrs.liris.common.util.{StringUtils, TimeUtils}

case class SubmitCommandFlags(
  @Flag(name = "name", help = "Run name")
  name: Option[String],
  @Flag(name = "tags", help = "Run tags (comma-separated)")
  tags: Option[String],
  @Flag(name = "notes", help = "Run notes")
  notes: Option[String],
  @Flag(name = "repeat", help = "Number of times to repeat each run")
  repeat: Option[Int],
  @Flag(name = "seed", help = "Seed to use for unstable operators")
  seed: Option[Long],
  @Flag(name = "q", help = "Print only identifiers")
  quiet: Boolean = false)

@Cmd(
  name = "submit",
  flags = Array(classOf[SubmitCommandFlags], classOf[AccioAgentFlags]),
  help = "Launch an Accio workflow.",
  allowResidue = true)
class SubmitCommand @Inject()(clientFactory: AgentClientFactory, factory: RunDefFactory)
  extends Command with StrictLogging {

  def execute(flags: FlagsProvider, out: Reporter): ExitCode = {
    if (flags.residue.isEmpty) {
      out.writeln("<error>[ERROR]</error> You must provide a run file or package specification.")
      ExitCode.CommandLineError
    } else {
      val opts = flags.as[SubmitCommandFlags]
      val elapsed = Stopwatch.start()

      val params = try {
        parseParams(flags.residue.tail)
      } catch {
        case e: IllegalArgumentException =>
          if (!opts.quiet) {
            out.writeln(s"<error>[ERROR]</error> Params argument parse error: ${e.getMessage}")
          }
          return ExitCode.CommandLineError
      }

      val defn = try {
        factory.create(
          flags.residue.head,
          name = opts.name,
          notes = opts.notes,
          tags = StringUtils.explode(opts.tags, ","),
          seed = opts.seed,
          params = params,
          repeat = opts.repeat)
      } catch {
        case e: ParsingException =>
          if (!opts.quiet) {
            out.writeln(s"<error>[ERROR]</error> Run definition parse error: ${e.getMessage}")
          }
          return ExitCode.DefinitionError
        case e: InvalidRunDefException =>
          if (!opts.quiet) {
            out.writeln(s"<error>[ERROR]</error> Run definition error: ${e.getMessage}")
          }
          return ExitCode.DefinitionError
        case e: AccioServerException =>
          if (!opts.quiet) {
            out.writeln(s"<error>[ERROR]</error> Server error: ${e.getMessage}")
          }
          return ExitCode.InternalError
      }

      val req = CreateRunRequest(defn, Utils.DefaultUser)
      val client = clientFactory.create(flags.as[AccioAgentFlags].addr)
      Await.result(client.createRun(req).liftToTry) match {
        case Return(resp) =>
          if (opts.quiet) {
            resp.ids.map(_.value).foreach(out.writeln)
          } else {
            resp.ids.foreach { runId =>
              out.writeln(s"<info>[OK]</info> Created run: ${runId.value}")
            }
            if (resp.ids.size > 1) {
              out.writeln(s"<info>[OK]</info> Created ${resp.ids.size} runs successfully")
            }
            out.writeln(s"<info>[OK]</info> Done in ${TimeUtils.prettyTime(elapsed())}.")
          }
          ExitCode.Success
        case Throw(e) =>
          if (!opts.quiet) {
            out.writeln(s"<error>[ERROR]</error> Server communication error: ${e.getMessage}")
          }
          ExitCode.InternalError
      }
    }
  }

  private[this] val ParamRegex = "([^=]+)=(.+)".r

  private def parseParams(params: Seq[String]): Map[String, String] = {
    params.map {
      case ParamRegex(paramName, value) => paramName -> value
      case str => throw new IllegalArgumentException(s"Invalid param (expected key=value): $str")
    }.toMap
  }
}