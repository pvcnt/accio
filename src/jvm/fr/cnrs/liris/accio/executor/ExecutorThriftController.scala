package fr.cnrs.liris.accio.executor

import java.nio.file.{Files, Path}
import java.util.UUID
import java.util.concurrent.Executors

import com.fasterxml.jackson.core.`type`.TypeReference
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.thrift.Controller
import com.twitter.util.{Future, FutureTask, Promise, Try}
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.core.api.{OpContext, Operator}
import fr.cnrs.liris.accio.core.framework._
import fr.cnrs.liris.accio.thrift.ExecutorService._
import fr.cnrs.liris.accio.thrift._
import fr.cnrs.liris.common.util.Requirements.requireState

import scala.reflect.ClassTag

class ExecutorThriftController(jobService: JobService.FinagledClient, opRegistry: OpRegistry, opFactory: OpFactory, workDir: Path, mapper: FinatraObjectMapper) extends Controller with ExecutorService.BaseServiceIface with StrictLogging {
  private[this] val executorService = Executors.newSingleThreadExecutor

  override val health = handle(Health) { _: Health.Args =>
    Future.Done
  }

  override val execute = handle(Execute) { args: Execute.Args =>
    val f = FutureTask(run(args.req))
    executorService.submit(f)
  }

  override val kill = handle(Kill) { args: Kill.Args =>

  }

  private def run(req: ExecuteJobRequest) = {
    logger.debug(s"Starting job ${req.jobId}")
    jobService.started(JobStartedRequest(req.jobId))
    //jobService.completed(JobCompletedRequest(req.jobId))
    logger.debug(s"Completed job ${req.jobId}")
  }

  private class ExecutorRunnable(req: ExecuteJobRequest) extends Promise[Unit] with Runnable {
    def run(): Unit = {
      update(Try {
        logger.debug(s"Starting job ${req.jobId}")
        jobService.started(JobStartedRequest(req.jobId))

        val opMeta = opRegistry(req.op)
        val operator = opFactory.create(opMeta)
        execute(operator, opMeta, req.seed, req.nodeName, req.inputs)

        //jobService.completed(JobCompletedRequest(req.jobId))
        logger.debug(s"Completed job ${req.jobId}")
      })
    }

    private def execute[In, Out](op: Operator[In, Out], opMeta: OpMeta, seed: Long, nodeName: String, inputs: collection.Map[String, ArtifactDatum]) = {
      val in = createInput(opMeta, node, run).asInstanceOf[In]
      val maybeSeed = if (op.isUnstable(in)) Some(run.seed) else None
      Files.createDirectories(workDir)
      val ctx = new OpContext(maybeSeed, workDir, node.name)
      val nodeStatus = executeOp(op, opMeta, ctx, in)
      val nodeKey = NodeKey(UUID.randomUUID().toString)
      artifactRepository.save(nodeKey, nodeStatus)
      (nodeKey, nodeStatus.successful)
    }

    private def createInput(opMeta: OpMeta, inputs: collection.Map[String, ArtifactDatum]): Any = {
      opMeta.inClass match {
        case None => Unit.box(Unit)
        case Some(inClass) =>
          val ctorArgs = opMeta.defn.inputs.map { argDef =>
            inputs.get(argDef.name) match {
              case None => argDef.defaultValue match {
                case Some(defaultValue) => defaultValue
                case None => throw new IllegalArgumentException(s"Missing non-optional input: ${argDef.name}")
              }
              case Some(artifact) =>
                val kind = DataType.parse(artifact.kind)

                if (argDef.isOptional) Some(value) else value
            }
          }
          inClass.getConstructors.head.newInstance(ctorArgs.map(_.asInstanceOf[AnyRef]): _*)
      }
    }

    private def parseJson(artifact: ArtifactDatum) = {
      val kind = DataType.parse(artifact.kind)
      kind match {
        case DataType.Boolean => parsePrimitive[Boolean](artifact.value)
        case DataType.Byte =>parsePrimitive[Byte](artifact.value)
        case DataType.Short => parsePrimitive[Short](artifact.value)
        case DataType.Integer => parsePrimitive[Integer](artifact.value)
        case DataType.Long => parsePrimitive[Long](artifact.value)
        case DataType.Double => parsePrimitive[Double](artifact.value)
        case DataType.String => parsePrimitive[String](artifact.value)
        case DataType.Location => asLocation(rawValue)
        case DataType.Timestamp => asTimestamp(rawValue)
        case DataType.Duration => asDuration(rawValue)
        case DataType.Distance => asDistance(rawValue)
        case DataType.Image => asImage(rawValue)
        case DataType.Dataset => asDataset(rawValue)
        case DataType.List(of) => asList(rawValue, of)
        case DataType.Set(of) => asSet(rawValue, of)
        case DataType.Map(ofKeys, ofValues) => asMap(rawValue, ofKeys, ofValues)
      }
      Artifact(artifact.name, kind, value)
    }
  }

  private def parsePrimitive[T: ClassTag](str: String) = {
    mapper.objectMapper.readValue[Primitive[T]](str, new TypeReference[Primitive[T]] {}).value
  }

}

case class Primitive[T](value: T)