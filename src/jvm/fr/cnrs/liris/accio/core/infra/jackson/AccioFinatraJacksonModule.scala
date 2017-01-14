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

package fr.cnrs.liris.accio.core.infra.jackson

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.module.SimpleModule
import com.twitter.finatra.json.modules.FinatraJacksonModule
import fr.cnrs.liris.accio.core.domain._

object AccioFinatraJacksonModule extends FinatraJacksonModule {
  override protected val serializationInclusion = JsonInclude.Include.NON_ABSENT

  override protected def additionalJacksonModules: Seq[Module] = Seq(AccioJacksonModule)
}

private object AccioJacksonModule extends SimpleModule {
  addDeserializer(classOf[Run], new ScroogeStructDeserializer[Run](Run))
  addDeserializer(classOf[RunId], new ScroogeStructDeserializer[RunId](RunId))
  addDeserializer(classOf[RunState], new ScroogeStructDeserializer[RunState](RunState))
  addDeserializer(classOf[Package], new ScroogeStructDeserializer[Package](Package))
  addDeserializer(classOf[NodeState], new ScroogeStructDeserializer[NodeState](NodeState))
  addDeserializer(classOf[Artifact], new ScroogeStructDeserializer[Artifact](Artifact))
  addDeserializer(classOf[Metric], new ScroogeStructDeserializer[Metric](Metric))
  addDeserializer(classOf[ErrorData], new ScroogeStructDeserializer[ErrorData](ErrorData))
  addDeserializer(classOf[Error], new ScroogeStructDeserializer[Error](Error))
  addDeserializer(classOf[RunLog], new ScroogeStructDeserializer[RunLog](RunLog))
  addDeserializer(classOf[NodeStatus], new ScroogeEnumDeserializer[NodeStatus](NodeStatus))
  addDeserializer(classOf[RunStatus], new ScroogeEnumDeserializer[RunStatus](RunStatus))

  addDeserializer(classOf[WorkflowId], new ScroogeStructDeserializer[WorkflowId](WorkflowId))
  addDeserializer(classOf[Workflow], new ScroogeStructDeserializer[Workflow](Workflow))
  addDeserializer(classOf[InputDef], new ScroogeUnionDeserializer[InputDef](InputDef))
  addDeserializer(classOf[Reference], new ScroogeStructDeserializer[Reference](Reference))

  addDeserializer(classOf[Value], new ScroogeStructDeserializer[Value](Value))
  addDeserializer(classOf[User], new ScroogeStructDeserializer[User](User))
  addDeserializer(classOf[Value], new ScroogeStructDeserializer[Value](Value))
  addDeserializer(classOf[DataType], new ScroogeStructDeserializer[DataType](DataType))
  addDeserializer(classOf[AtomicType], new ScroogeEnumDeserializer[AtomicType](AtomicType))

  addSerializer(new ScroogeStructSerializer)
  addSerializer(new ScroogeEnumSerializer)
}