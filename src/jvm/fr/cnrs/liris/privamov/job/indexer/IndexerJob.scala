package fr.cnrs.liris.privamov.job.indexer

import com.github.nscala_time.time.Imports._
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import fr.cnrs.liris.accio.core.dataset.DatasetEnv
import fr.cnrs.liris.accio.core.io.{CabspottingSource, GeolifeSource}
import fr.cnrs.liris.common.flags.{Flag, FlagsParser}
import org.elasticsearch.common.settings.Settings

import scala.reflect.runtime.universe._

case class IndexerFlags(
    @Flag(name = "type")
    typ: String,
    @Flag(name = "timezone")
    timezone: String = "Europe/Paris",
    @Flag(name = "elastic_uri")
    elasticUri: String = "localhost:9300")

object IndexerJobMain extends IndexerJob

class IndexerJob {
  def main(args: Array[String]): Unit = {
    val flagsParser = FlagsParser(allowResidue = true, typeOf[IndexerFlags])
    flagsParser.parseAndExitUponError(args)
    val flags = flagsParser.as[IndexerFlags]

    val elasticClient = ElasticClient.transport(
      Settings.settingsBuilder.put("client.transport.ping_timeout", "30s").build,
      ElasticsearchClientUri(flags.elasticUri))

    val env = new DatasetEnv(1)
    val indexer = new Indexer(elasticClient, indexName = "event")
    try {
      flagsParser.residue.foreach { url =>
        val dataset = createDataset(env, flags.typ, url)
        indexer.run(dataset, flags.typ, DateTimeZone.forID(flags.timezone))
      }
    } finally {
      elasticClient.close()
      env.stop()
    }
  }

  private def createDataset(env: DatasetEnv, typ: String, url: String) = {
    val source = typ match {
      case "cabspotting" => CabspottingSource(url)
      case "geolife" => GeolifeSource(url)
      case _ => throw new IllegalArgumentException(s"Unknown type '$typ'")
    }
    env.read(source)
  }
}