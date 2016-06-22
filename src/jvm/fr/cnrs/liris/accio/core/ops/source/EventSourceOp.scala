package fr.cnrs.liris.accio.core.ops.source

import fr.cnrs.liris.accio.core.dataset.{Dataset, DatasetEnv}
import fr.cnrs.liris.accio.core.framework.{Op, Source}
import fr.cnrs.liris.accio.core.io.{CabspottingSource, CsvSource, GeolifeSource}
import fr.cnrs.liris.accio.core.model.Trace
import fr.cnrs.liris.accio.core.param.Param
import fr.cnrs.liris.common.util.FileUtils

@Op(
  category = "source",
  help = "Read a dataset of traces in CSV format",
  ephemeral = true
)
case class EventSourceOp(
    @Param(help = "Dataset URL")
    url: String,
    @Param(help = "Kind of dataset")
    kind: String = "csv",
    @Param(help = "Sampling ratio")
    sample: Option[Double],
    @Param(help = "User identifiers")
    users: Seq[String]
) extends Source {

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