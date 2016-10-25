package fr.cnrs.liris.privamov.ops

import com.google.inject.Inject
import fr.cnrs.liris.accio.core.api._
import fr.cnrs.liris.common.util.FileUtils
import fr.cnrs.liris.privamov.core.sparkle._

@Op(
  category = "source",
  help = "Read a dataset of traces.")
class EventSourceOp @Inject()(env: SparkleEnv) extends Operator[EventSourceIn, EventSourceOut] with SparkleOperator {

  override def execute(in: EventSourceIn, ctx: OpContext): EventSourceOut = {
    val source = in.kind match {
      case "csv" => CsvSource(FileUtils.replaceHome(in.url))
      case "cabspotting" => CabspottingSource(FileUtils.replaceHome(in.url))
      case "geolife" => GeolifeSource(FileUtils.replaceHome(in.url))
      case _ => throw new IllegalArgumentException(s"Unknown kind: ${in.kind}")
    }
    val output = if (in.kind != "csv" || in.sample.nonEmpty || in.users.nonEmpty) {
      var data = env.read(source)
      in.sample.foreach { sample =>
        data = data.sample(withReplacement = false, sample, ctx.seed)
      }
      if (in.users.nonEmpty) {
        data = data.restrict(in.users.toSet)
      }
      write(data, ctx.workDir)
    } else {
      Dataset(in.url, format = "csv")
    }
    EventSourceOut(output)
  }
}

case class EventSourceIn(
  @Arg(help = "Dataset URL") url: String,
  @Arg(help = "Kind of dataset") kind: String = "csv",
  @Arg(help = "Sampling ratio") sample: Option[Double],
  @Arg(help = "Users to include") users: Seq[String] = Seq.empty)

case class EventSourceOut(@Arg(help = "Source dataset") data: Dataset)