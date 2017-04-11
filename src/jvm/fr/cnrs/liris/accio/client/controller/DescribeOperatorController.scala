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

package fr.cnrs.liris.accio.client.controller

import com.twitter.util.Future
import fr.cnrs.liris.accio.agent.{AgentService$FinagleClient, GetOperatorRequest}
import fr.cnrs.liris.accio.core.api._
import fr.cnrs.liris.accio.runtime.event.Reporter
import fr.cnrs.liris.common.util.StringUtils
import fr.cnrs.liris.dal.core.api.{DataTypes, Values}

class DescribeOperatorController extends DescribeController[OpDef] {
  override def retrieve(id: String, client: AgentService$FinagleClient): Future[OpDef] = {
    client.getOperator(GetOperatorRequest(id)).map { resp =>
      resp.result match {
        case None => throw new NoResultException
        case Some(opDef) => opDef
      }
    }
  }

  override def print(out: Reporter, opDef: OpDef): Unit = {
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

  private def printInputs(out: Reporter, opDef: OpDef) = {
    out.outErr.printOutLn()
    out.outErr.printOutLn(s"Available inputs")
    opDef.inputs.foreach { argDef =>
      out.outErr.printOut(s"  ${argDef.name} (${DataTypes.toString(argDef.kind)}")
      if (argDef.defaultValue.isDefined) {
        out.outErr.printOut(s"; default: ${Values.toString(argDef.defaultValue.get)}")
      }
      if (argDef.isOptional) {
        out.outErr.printOut("; optional")
      }
      out.outErr.printOut(")")
      argDef.help.foreach(help => out.outErr.printOut(": " + help))
      out.outErr.printOutLn()
    }
  }

  private def printOutputs(out: Reporter, opDef: OpDef) = {
    out.outErr.printOutLn()
    if (opDef.outputs.nonEmpty) {
      out.outErr.printOutLn("Available outputs")
      opDef.outputs.foreach { outputDef =>
        out.outErr.printOut(s"  - ${outputDef.name} (${DataTypes.toString(outputDef.kind)})")
        out.outErr.printOutLn()
        outputDef.help.foreach(help => out.outErr.printOutLn(StringUtils.paragraphFill(help, 80, 4)))
      }
    }
  }
}