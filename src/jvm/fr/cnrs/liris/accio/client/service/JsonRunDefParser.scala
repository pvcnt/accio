package fr.cnrs.liris.accio.client.service

import java.io.FileInputStream
import java.nio.file.Path

import com.google.inject.Inject
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.validation.Min

import scala.util.control.NonFatal

private[service] class JsonRunDefParser @Inject()(mapper: FinatraObjectMapper) {
  @throws[ParsingException]
  def parse(path: Path): JsonRunDef = {
    val file = path.toFile
    if (!file.exists || !file.canRead) {
      throw new ParsingException(s"Cannot read run definition file ${path.toAbsolutePath}")
    }
    val fis = new FileInputStream(file)
    try {
      mapper.parse[JsonRunDef](fis)
    } catch {
      case NonFatal(e) => throw new ParsingException("Error while parsing run definition", e)
    } finally {
      fis.close()
    }
  }
}

private[service] case class JsonRunDef(
  pkg: String,
  environment: Option[String],
  name: Option[String],
  notes: Option[String],
  tags: Set[String] = Set.empty,
  seed: Option[Long],
  params: Map[String, Exploration] = Map.empty,
  @Min(1) repeat: Option[Int])

