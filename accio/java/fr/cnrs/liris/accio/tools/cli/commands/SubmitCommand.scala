/*
 * Accio is a platform to launch computer science experiments.
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

package fr.cnrs.liris.accio.tools.cli.commands

import com.twitter.util.Future
import fr.cnrs.liris.accio.server.CreateJobRequest
import fr.cnrs.liris.accio.api.Values
import fr.cnrs.liris.accio.api.thrift.{Job, NamedValue}
import fr.cnrs.liris.accio.dsl.json.JsonWorkflowParser
import fr.cnrs.liris.accio.tools.cli.event.{Event, EventKind, Reporter}
import fr.cnrs.liris.util.{FileUtils, StringUtils}

final class SubmitCommand extends Command with ClientCommand {
  private[this] val titleFlag = flag[String]("title", "Job name")
  private[this] val tagsFlag = flag[String]("tags", "Job tags (comma-separated)")
  private[this] val seedFlag = flag[Long]("seed", "Seed to use for controlled randomness")

  override def name = "submit"

  override def help = "Launch an Accio job."

  override def allowResidue = true

  override def execute(residue: Seq[String], env: CommandEnvironment): Future[ExitCode] = {
    if (residue.isEmpty) {
      env.reporter.handle(Event.error("You must provide the path to a job file."))
      return Future.value(ExitCode.CommandLineError)
    }
    try {
      parseAndSubmit(residue.head, residue.tail, env.reporter)
    } catch {
      case e: IllegalArgumentException =>
        env.reporter.handle(Event.error(s"Params argument parse error: ${e.getMessage}"))
        Future.value(ExitCode.CommandLineError)
    }
  }

  private def parseAndSubmit(uri: String, residue: Seq[String], reporter: Reporter): Future[ExitCode] = {
    val file = FileUtils.expandPath(uri).toFile
    if (!file.canRead) {
      reporter.handle(Event.error(s"Cannot read workflow definition file: ${file.getAbsolutePath}"))
      return Future.value(ExitCode.DefinitionError)
    }

    val parser = new JsonWorkflowParser
    parser
      .parse(file)
      .map(merge(_, residue))
      .flatMap(job => client.createJob(CreateJobRequest(job)))
      .map { resp =>
        resp.warnings.foreach { violation =>
          reporter.handle(Event(EventKind.Warning, s"${violation.message} (at ${violation.field})"))
        }
        reporter.handle(Event.info(s"Created job ${resp.job.name}"))
        ExitCode.Success
      }
  }

  private def merge(job: Job, residue: Seq[String]) = {
    var newJob = job
    titleFlag.get.foreach(title => newJob = newJob.copy(title = Some(title)))
    newJob = newJob.copy(tags = newJob.tags ++ StringUtils.explode(tagsFlag.get, ","))
    seedFlag.get.foreach(seed => newJob = newJob.copy(seed = seed))
    if (residue.nonEmpty) {
      val params = parseParams(residue)
      newJob = newJob.copy(params = params)
    }
    newJob
  }

  private def parseParams(residue: Seq[String]): Seq[NamedValue] = {
    residue.map { str =>
      val pos = str.indexOf('=')
      if (pos >= -1) {
        NamedValue(str.take(pos), Values.encodeString(str.drop(pos + 1)))
      } else {
        throw new IllegalArgumentException(s"Invalid param (expected key=value): $str")
      }
    }
  }
}