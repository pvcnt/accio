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

import com.google.inject.Inject
import com.twitter.finatra.json.internal.caseclass.exceptions.CaseClassMappingException
import com.twitter.finatra.validation.ErrorCode._
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.core.framework.{IllegalWorkflowException, _}
import fr.cnrs.liris.common.flags.{Flag, FlagsProvider}

case class ValidateOptions(
  @Flag(name = "keep-going", help = "Whether to continue validating other files once an error occurred")
  keepGoing: Boolean = true)

@Cmd(
  name = "validate",
  help = "Validate the syntax of Accio configuration files.",
  flags = Array(classOf[ValidateOptions]),
  allowResidue = true)
class ValidateCommand @Inject()(experimentFactory: ExperimentFactory, experimentParser: ExperimentParser, workflowFactory: WorkflowFactory, workflowParser: WorkflowParser)
  extends Command with StrictLogging {

  def execute(flags: FlagsProvider, out: Reporter): ExitCode = {
    if (flags.residue.isEmpty) {
      out.writeln("<error>Specify one or multiple files to validate as arguments.</error>")
      ExitCode.CommandLineError
    } else {
      val opts = flags.as[ValidateOptions]
      var valid = true
      var i = 0
      while (i < flags.residue.size) {
        if (validate(flags.residue(i), out)) {
          i += 1
        } else {
          valid = false
          i = if (opts.keepGoing) i + 1 else flags.residue.size
        }
      }
      if (valid) {
        out.writeln(s"<info>${flags.residue.size} valid files.</info>")
        ExitCode.Success
      } else {
        ExitCode.ValidateFailure
      }
    }
  }

  /**
   * Validate a single definition file. If the definition is invalid, more information will be printed using
   * the `out` reporter.
   *
   * @param uri URI to an experiment or workflow definition.
   * @param out Reporter where to write output.
   * @return True if the definition is valid, false otherwise.
   */
  private def validate(uri: String, out: Reporter): Boolean = {
    if (experimentParser.canRead(uri)) {
      validateExperiment(uri, out)
    } else if (workflowParser.canRead(uri)) {
      validateWorkflow(uri, out)
    } else {
      out.writeln(s"<error>- Error validating $uri: Neither an experiment or a workflow file.</error>")
      false
    }
  }

  /**
   * Validate a single experiment definition file. If the definition is invalid, more information will be printed
   * using the `out` reporter.
   *
   * @param uri URI to an experiment definition.
   * @param out Reporter where to write output.
   * @return True if the definition is valid, false otherwise.
   */
  private def validateExperiment(uri: String, out: Reporter) = {
    try {
      experimentFactory.create(uri, ExperimentArgs())
      true
    } catch {
      case e: IllegalExperimentException =>
        out.writeln(s"<error>- Error validating experiment $uri</error>")
        explainException(e, out, level = 1)
        false
      case e: IllegalWorkflowException =>
        out.writeln(s"<error>- Error while validating workflow of experiment $uri</error>")
        out.writeln(s"<error>Try 'accio validate $uri' for more information.</error>")
        false
    }
  }

  /**
   * Validate a single workflow definition file. If the definition is invalid, more information will be printed
   * using the `out` reporter.
   *
   * @param uri URI to a workflow definition.
   * @param out Reporter where to write output.
   * @return True if the definition is valid, false otherwise.
   */
  private def validateWorkflow(uri: String, out: Reporter) = {
    try {
      workflowFactory.create(uri, User.Default)
      true
    } catch {
      case e: IllegalWorkflowException =>
        out.writeln(s"<error>- Error validating workflow $uri</error>")
        explainException(e, out, level = 1)
        false
    }
  }

  /**
   * Print human-readable information about an exception, recursively.
   *
   * @param e
   * @param out
   * @param level
   */
  private def explainException(e: Throwable, out: Reporter, level: Int): Unit = {
    Option(e) match {
      case None => // No cause, do nothing.
      case Some(e: CaseClassMappingException) =>
        // Special handling for JSON parse exception, print meaningful error message.
        e.errors.foreach { err =>
          val more = err.reason.code match {
            case InvalidCountryCodes(codes) => s"Invalid country code, must be one of ${codes.mkString(", ")}"
            case InvalidTimeGranularity(time, targetGranularity) => s"Invalid time granularity, must be $targetGranularity"
            case InvalidUUID(uuid) => "Invalid UUID"
            case InvalidValues(invalid, valid) => s"Invalid values, must belong to ${valid.mkString(", ")}"
            case JsonProcessingError(cause) => cause.getMessage
            case RequiredFieldMissing => "Required field is missing"
            case SizeOutOfRange(size, min, max) => s"Size out of range, must be between $min and $max"
            case TimeNotFuture(time) => "Time must be future"
            case TimeNotPast(time) => "Time must be past"
            case ValueCannotBeEmpty => "Value cannot be empty"
            case ValueOutOfRange(value, min, max) => s"Value out of range, must be between $min and $max"
            case ValueTooLarge(maxValue, value) => s"Value is too large, must be at most $maxValue"
            case ValueTooSmall(minValue, value) => s"Value is too small, must be at least $minValue"
            case Unknown => "Unknown error"
          }
          out.writeln(s"<error>${" " * level * 2}Error at ${err.path.prettyString}: ${err.reason.message}</error>")
          out.writeln(s"<error>${" " * level * 2}$more</error>")
        }
      case Some(e: Throwable) =>
        // General handling of exceptions, print their message and recurse to print parent cause.
        out.writeln(s"<error>${" " * level * 2}Caused by: ${e.getMessage.trim}</error>")
        explainException(e.getCause, out, level + 1)
    }
  }
}