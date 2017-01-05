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

package fr.cnrs.liris.accio.core.runtime

import java.nio.file.{Files, Path}
import java.util.UUID

import com.google.inject.Inject
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.core.api.{OpContext, Operator}
import fr.cnrs.liris.accio.core.framework.{Metric, _}
import fr.cnrs.liris.common.util.Requirements._
import org.joda.time.DateTime

import scala.util.control.NonFatal

/**
 *
 *
 * @param artifactRepository Artifact repository.
 * @param opRegistry         Operator registry.
 * @param opFactory          Operator factory.
 */
class NodeExecutor @Inject()(artifactRepository: ArtifactRepository, opRegistry: OpRegistry, opFactory: OpFactory)
  extends StrictLogging {

  def execute(run: Run, node: Node, workDir: Path): (NodeKey, Boolean) = {
    val opMeta = opRegistry(node.op)
    val operator = opFactory.create(opMeta)
    execute(operator, opMeta, run, node, workDir)
  }

  private def execute[In, Out](op: Operator[In, Out], opMeta: OpMeta, run: Run, node: Node, workDir: Path): (NodeKey, Boolean) = {
    val in = createInput(opMeta, node, run).asInstanceOf[In]
    val maybeSeed = if (op.isUnstable(in)) Some(run.seed) else None
    Files.createDirectories(workDir)
    val ctx = new OpContext(maybeSeed, workDir, node.name)
    val nodeStatus = executeOp(op, opMeta, ctx, in)
    val nodeKey = NodeKey(UUID.randomUUID().toString)
    artifactRepository.save(nodeKey, nodeStatus)
    (nodeKey, nodeStatus.successful)
  }

  private def executeOp[In, Out](op: Operator[In, Out], opMeta: OpMeta, ctx: OpContext, in: In): NodeStatus = {
    val startedAt = DateTime.now
    val instrumentor = new Instrumentor
    Instrumentor.recordOutErr()
    val res = instrumentor {
      try {
        Left(op.execute(in, ctx))
      } catch {
        case NonFatal(e) => Right(ExceptionData(e))
      }
    }
    val completedAt = DateTime.now
    val stdout = Instrumentor.stdoutAsString
    val stderr = Instrumentor.stderrAsString
    Instrumentor.restoreOutErr()

    val (artifacts, exception) = res match {
      case Left(out) => (extractArtifacts(opMeta, ctx.nodeName, out), None)
      case Right(ex) => (Seq.empty[Artifact], Some(ex))
    }
    val exitCode = exception.map(_.className.hashCode).getOrElse(0)

    val metrics = Seq(
      Metric("memory_used_bytes", instrumentor.memoryUsed.inBytes),
      Metric("memory_reserved_bytes", instrumentor.memoryReserved.inBytes),
      Metric("cpu_time_nanos", instrumentor.cpuTime.inNanoseconds),
      Metric("user_time_nanos", instrumentor.userTime.inNanoseconds),
      Metric("system_time_nanos", instrumentor.userTime.inNanoseconds))

    NodeStatus(
      startedAt = startedAt,
      completedAt = completedAt,
      successful = exception.isEmpty,
      stdout = stdout,
      stderr = stderr,
      exitCode = exitCode,
      exception = exception,
      artifacts = artifacts,
      metrics = metrics)
  }

  private def createInput(opMeta: OpMeta, node: Node, run: Run): Any =
    opMeta.inClass match {
      case None => Unit.box(Unit)
      case Some(inClass) =>
        val ctorArgs = opMeta.defn.inputs.map { argDef =>
          node.inputs.get(argDef.name) match {
            case None => throw new IllegalArgumentException(s"Missing input: ${node.name}/${argDef.name}")
            case Some(ValueInput(value)) => value
            case Some(ReferenceInput(ref)) =>
              //TODO: strenghten this.
              val value = artifactRepository.get(run.status.completedNodes(ref.node)).get.artifacts.find(_.name == ref.port)
              if (argDef.isOptional) Some(value) else value
            case Some(ParamInput(paramName, defaultValue)) =>
              val v = run.params.get(paramName).orElse(defaultValue)
              requireState(v.isDefined, s"There should be either a $paramName param or a default value for ${node.name}/${argDef.name}")
              v.get
          }
        }
        inClass.getConstructors.head.newInstance(ctorArgs.map(_.asInstanceOf[AnyRef]): _*)
    }

  private def extractArtifacts(opMeta: OpMeta, nodeName: String, out: Any): Seq[Artifact] =
    opMeta.outClass match {
      case None => Seq.empty
      case Some(outClass) =>
        opMeta.defn.outputs.map { argDef =>
          val value = outClass.getMethod(argDef.name).invoke(out)
          Artifact(argDef.name, argDef.kind, value)
        }
    }
}