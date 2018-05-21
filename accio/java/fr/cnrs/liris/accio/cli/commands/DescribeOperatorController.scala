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

package fr.cnrs.liris.accio.cli.commands

import com.twitter.util.Future
import fr.cnrs.liris.accio.server.GetOperatorRequest
import fr.cnrs.liris.util.StringUtils

class DescribeOperatorController extends DescribeController[Operator] {
  override def retrieve(id: String, client: AgentService.MethodPerEndpoint): Future[Operator] = {
    client
      .getOperator(GetOperatorRequest(id))
      .map(_.operator)
  }

  override def print(out: Reporter, opDef: Operator): Unit = {
    out.outErr.printOutLn(s"Operator: ${opDef.name} (${opDef.category})")
    opDef.deprecation.foreach { deprecation =>
      out.outErr.printOutLn()
      out.outErr.printOutLn(s"<error>Deprecated: $deprecation</error>")
    }
    opDef.description.foreach { description =>
      out.outErr.printOutLn()
      out.outErr.printOutLn(StringUtils.paragraphFill(description, 80))
    }
    printInputs(out, opDef)
    printOutputs(out, opDef)
  }

  private def printInputs(out: Reporter, opDef: Operator): Unit = {
    out.outErr.printOutLn()
    out.outErr.printOutLn(s"Available inputs")
    opDef.inputs.foreach { argDef =>
      out.outErr.printOut(s"  - ${argDef.name} [${DataTypes.stringify(argDef.dataType)}")
      if (argDef.defaultValue.isDefined) {
        out.outErr.printOut(s"; default: ${Values.stringify(argDef.defaultValue.get)}")
      }
      if (argDef.isOptional || argDef.defaultValue.isDefined) {
        out.outErr.printOut("; optional")
      }
      out.outErr.printOut("]")
      argDef.help.foreach(help => out.outErr.printOut(": " + help))
      out.outErr.printOutLn()
    }
  }

  private def printOutputs(out: Reporter, opDef: Operator): Unit = {
    out.outErr.printOutLn()
    if (opDef.outputs.nonEmpty) {
      out.outErr.printOutLn("Available outputs")
      opDef.outputs.foreach { argDef =>
        out.outErr.printOut(s"  - ${argDef.name} [${DataTypes.stringify(argDef.dataType)}]")
        argDef.help.foreach(help => out.outErr.printOut(": " + help))
        out.outErr.printOutLn()
      }
    }
  }
}