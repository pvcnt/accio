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

package fr.cnrs.liris.common.getter

import java.net.{MalformedURLException, URL}
import java.nio.file.Path

import com.typesafe.scalalogging.LazyLogging

class S3Detector extends Detector with LazyLogging {
  override def detect(str: String, pwd: Option[Path]): Option[DetectedURI] = {
    if (str.contains(".amazonaws.com/")) {
      detectHttp(str)
    } else {
      None
    }
  }

  private def detectHttp(str: String) = {
    val parts = str.split('/')
    if (parts.length < 2) {
      logger.error(s"URL is not a valid S3 URL (got $str)")
      None
    } else {
      val hostParts = parts.head.split('.')
      if (hostParts.length == 3) {
        detectPathStyle(hostParts(0), parts.tail)
      } else if (hostParts.length == 4) {
        detectVhostStyle(hostParts(1), hostParts(0), parts.tail)
      } else {
        logger.error(s"URL is not a valid S3 URL (got $str)")
        None
      }
    }
  }

  private def detectWithUrl(urlString: String) = {
    val url = try {
      new URL(urlString)
    } catch {
      case e: MalformedURLException => throw new DetectorException(s"Error parsing S3 URL", e)
    }
    DetectedURI(rawUri = url.toURI, getter = Some("s3"))
  }

  private def detectPathStyle(region: String, parts: Array[String]) = {
    Some(detectWithUrl(s"https://$region.amazonaws.com/${parts.mkString("/")}"))
  }

  private def detectVhostStyle(region: String, bucket: String, parts: Array[String]) = {
    Some(detectWithUrl(s"https://$region.amazonaws.com/$bucket/${parts.mkString("/")}"))
  }
}
