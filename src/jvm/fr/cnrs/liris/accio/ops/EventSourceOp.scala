package fr.cnrs.liris.accio.ops

import fr.cnrs.liris.accio.core.dataset.{Dataset, DatasetEnv}
import fr.cnrs.liris.accio.core.framework.{Op, OpContext, Out, Source}
import fr.cnrs.liris.accio.core.io.{CabspottingSource, CsvSource, GeolifeSource}
import fr.cnrs.liris.accio.core.model.Trace
import fr.cnrs.liris.accio.ops.EventSourceOp._
import fr.cnrs.liris.accio.core.param.Param
import fr.cnrs.liris.common.util.FileUtils

@Op(
  category = "source",
  help = "Read a dataset of traces in CSV format")
case class EventSourceOp(
  @Param(help = "Dataset URL") url: String,
  @Param(help = "Kind of dataset") kind: String = "csv",
  @Param(help = "Sampling ratio") sample: Option[Double],
  @Param(help = "Users to include") users: Seq[String] = Seq.empty
) extends Source[Output] {

  override def execute(in: Unit, ctx: OpContext): Output = {
    Output(get(ctx.env))
  }

  override def get(env: DatasetEnv): Dataset[Trace] = {
    val source = kind match {
      case "csv" => CsvSource(FileUtils.replaceHome(url))
      case "cabspotting" => CabspottingSource(FileUtils.replaceHome(url))
      case "geolife" => GeolifeSource(FileUtils.replaceHome(url))
      case _ => throw new IllegalArgumentException(s"Unknown kind: $kind")
    }
    var data = env.read(source)
    if (sample.nonEmpty) {
      data = data.sample(sample.get)
    }
    if (users.nonEmpty) {
      data = data.restrict(users.toSet)
    }
    data
  }
}

object EventSourceOp {

  case class Output(@Out(help = "Source dataset") data: Dataset[Trace])

}