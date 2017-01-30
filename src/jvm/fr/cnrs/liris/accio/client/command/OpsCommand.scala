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
import com.twitter.util.Await
import fr.cnrs.liris.accio.agent.{GetOperatorRequest, ListOperatorsRequest}
import fr.cnrs.liris.accio.core.domain.{OpDef, Utils}
import fr.cnrs.liris.common.cli._
import fr.cnrs.liris.common.flags.FlagsProvider
import fr.cnrs.liris.common.util.StringUtils

@Cmd(
  name = "ops",
  help = "List operators.",
  flags = Array(classOf[AccioAgentFlags]),
  allowResidue = true)
class OpsCommand @Inject()(clientFactory: AgentClientFactory) extends Command {

  override def execute(flags: FlagsProvider, out: Reporter): ExitCode = {
    if (flags.residue.size > 1) {
      out.writeln("<error>[ERROR]</error> You can specify only one operator name.")
      ExitCode.CommandLineError
    } else {
      val client = clientFactory.create(flags.as[AccioAgentFlags].addr)
      try {
        if (flags.residue.nonEmpty) {
          Await.result(client.getOperator(GetOperatorRequest(flags.residue.head))).result match {
            case Some(opDef) =>
              print(opDef, out)
              ExitCode.Success
            case None =>
              out.writeln(s"<error>[ERROR]</error> Unknown operator: ${flags.residue.head}")
              ExitCode.CommandLineError
          }
        } else {
          val ops = Await.result(client.listOperators(ListOperatorsRequest())).results
          printSummary(ops, out)
          ExitCode.Success
        }
      } catch {
        case e: AccioServerException =>
          out.writeln(s"<error>[ERROR]</error> Server error: ${e.getMessage}")
          ExitCode.InternalError
      }
    }
  }

  private def printSummary(ops: Seq[OpDef], out: Reporter) = {
    val maxLength = ops.map(_.name.length).max
    ops.sortBy(_.name).groupBy(_.category).foreach { case (category, categoryOps) =>
      out.writeln(s"<info>Operators in $category category:</info>")
      categoryOps.foreach { op =>
        val padding = " " * (maxLength - op.name.length)
        out.writeln(s"  <comment>${op.name}</comment>$padding ${op.help.getOrElse("")}")
      }
    }
  }

  private def print(opDef: OpDef, out: Reporter) = {
    out.writeln(s"<comment>${opDef.name}</comment> (${opDef.category})")
    out.writeln()
    opDef.deprecation.foreach { deprecation =>
      out.writeln(s"<error>Deprecated: $deprecation</error>")
      out.writeln()
    }
    opDef.help.foreach { help =>
      out.writeln(help)
      out.writeln()
    }
    opDef.description.foreach { description =>
      out.writeln(StringUtils.paragraphFill(description, 80))
      out.writeln()
    }
    printOpInputs(out, opDef)
    printOpOutputs(out, opDef)
  }

  private def printOpInputs(out: Reporter, opDef: OpDef) = {
    out.writeln(s"<info>Available inputs:</info>")
    opDef.inputs.foreach { argDef =>
      out.write(s"  - ${argDef.name} (${Utils.describe(argDef.kind)}")
      if (argDef.defaultValue.isDefined) {
        out.write(s"; default: ${argDef.defaultValue.get}")
      }
      if (argDef.isOptional) {
        out.write("; optional")
      }
      out.write(")")
      out.writeln()
      argDef.help.foreach(help => out.writeln(StringUtils.paragraphFill(help, 80, 4)))
    }
  }

  private def printOpOutputs(out: Reporter, opDef: OpDef) = {
    if (opDef.outputs.nonEmpty) {
      out.writeln("<info>Available outputs:</info>")
      opDef.outputs.foreach { outputDef =>
        out.write(s"  - ${outputDef.name} (${Utils.describe(outputDef.kind)})")
        out.writeln()
        outputDef.help.foreach(help => out.writeln(StringUtils.paragraphFill(help, 80, 4)))
      }
    }
  }
}