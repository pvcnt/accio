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

package fr.cnrs.liris.accio.cli

import java.nio.file.{Files, Path, Paths}

import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConverters._

/**
 * Parser for .acciorc files.
 */
class AccioRcParser extends LazyLogging {

  def parse(customPath: Option[Path], config: Option[String], cmdName: String): Seq[String] =
    getAccioRcPath(customPath) match {
      case None => Seq.empty
      case Some(path) =>
        logger.info(s"Loaded .acciorc file: ${path.toAbsolutePath}")
        val parsedFile = parseAccioRc(path)
        val cmdNameWithConfig = s"$cmdName:${config.getOrElse("")}"
        val applicableLines = parsedFile.filter(strs => strs.head == cmdName || strs.head == cmdNameWithConfig)
        applicableLines.flatMap(_.drop(1))
    }

  private def getAccioRcPath(customPath: Option[Path]) =
    customPath match {
      case Some(p) =>
        if (p.toFile.exists) {
          Some(p)
        } else {
          logger.warn(s"Specified non-existant .acciorc file: ${p.toAbsolutePath}")
          None
        }
      case None =>
        val possiblePaths = Seq(
          Paths.get(".acciorc"),
          Paths.get(sys.props("user.home")).resolve(".acciorc"),
          Paths.get("/etc/accio.acciorc"))
        possiblePaths.find(_.toFile.exists)
    }

  private def parseAccioRc(path: Path): Seq[Seq[String]] = {
    val lines = Files.readAllLines(path).asScala.map(_.trim).filter(s => s.nonEmpty && !s.startsWith("#"))
    lines.flatMap { line =>
      //TODO: use Bourne Shell tokenization rules.
      val tokens = line.split(" ").map(_.trim).toSeq
      if (tokens.head == "import") {
        tokens.tail.flatMap(s => parseAccioRc(Paths.get(s)))
      } else {
        Seq(tokens)
      }
    }
  }
}