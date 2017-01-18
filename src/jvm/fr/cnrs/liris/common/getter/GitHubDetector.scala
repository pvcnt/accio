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

import java.net.{MalformedURLException, URI, URL}
import java.nio.file.Path

class GitHubDetector extends Detector {
  override def detect(str: String, pwd: Option[Path]): Option[DetectedURI] = {
    if (str.startsWith("github.com/")) {
      Some(detectHttp(str))
    } else if (str.startsWith("git@github.com:")) {
      Some(detectSsh(str))
    } else {
      None
    }
  }

  private def detectHttp(str: String) = {
    val qidx = str.indexOf("?")
    val query = if (qidx > -1) str.drop(qidx) else ""

    val parts = (if (qidx > -1) str.take(qidx) else str).split('/')
    if (!parts(2).endsWith(".git")) {
      parts(2) += ".git"
    }
    if (parts.length < 3) {
      throw new DetectorException(s"GitHub URLs should be github.com/username/repo (got $str)")
    }

    val url = try {
      new URL(s"https://${parts.take(3).mkString("/")}$query")
    } catch {
      case e: MalformedURLException => throw new DetectorException(s"Error parsing GitHub URL", e)
    }
    val subdir = if (parts.length > 3) Some(parts.drop(3).mkString("/")) else None
    DetectedURI(rawUri = url.toURI, getter = Some("git"), subdir = subdir)
  }

  private def detectSsh(str: String) = {
    val qidx = str.indexOf("?")
    val query = if (qidx > -1) str.drop(qidx + 1) else null

    val idx = str.indexOf(":")
    val parts = (if (qidx > -1) str.substring(idx + 1, qidx) else str.drop(idx + 1)).split('/').filter(_.nonEmpty)
    if (parts.length < 2) {
      throw new DetectorException(s"GitHub URLs should be git@github.com:username/repo (got $str)")
    }

    val path = s"/${parts.take(2).mkString("/")}"
    val uri = try {
      new URI("ssh", "git", "github.com", -1, path, query, null)
    } catch {
      case e: MalformedURLException => throw new DetectorException(s"Error parsing GitHub URL", e)
    }
    val subdir = if (parts.length > 2) Some(parts.drop(2).mkString("/")) else None
    DetectedURI(rawUri = uri, getter = Some("git"), subdir = subdir)
  }
}