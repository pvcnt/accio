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

package fr.cnrs.liris.accio.viz

import java.nio.file.Paths

import com.google.inject.{Inject, Singleton}
import com.twitter.finatra.http.Controller
import com.twitter.finatra.request.RouteParam
import fr.cnrs.liris.accio.core.framework.ReportRepository

case class ListExperimentsRequest()

case class GetExperimentRequest(@RouteParam id: String)

case class GetRunRequest(@RouteParam id: String)

@Singleton
class ApiController @Inject()(repository: ReportRepository) extends Controller {
  private[this] val workDir = Paths.get(sys.props("user.home")).resolve("data/experiments")

  get("/api/experiment") { req: ListExperimentsRequest =>
    repository.list(workDir)
  }

  get("/api/experiment/:id") { req: GetExperimentRequest =>
    repository.readExperiment(workDir, req.id)
  }

  get("/api/run/:id") { req: GetRunRequest =>
    repository.readRun(workDir, req.id)
  }
}