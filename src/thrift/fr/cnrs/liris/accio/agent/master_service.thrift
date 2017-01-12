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

namespace java fr.cnrs.liris.accio.agent

include "fr/cnrs/liris/accio/core/application/master.thrift"

service MasterService {
  master.GetOperatorResponse getOperator(1: master.GetOperatorRequest req);

  master.ListOperatorsResponse listOperators(1: master.ListOperatorsRequest req);

  master.PushWorkflowResponse pushWorkflow(1: master.PushWorkflowRequest req);

  master.GetWorkflowResponse getWorkflow(1: master.GetWorkflowRequest req);

  master.ListWorkflowsResponse listWorkflows(1: master.ListWorkflowsRequest req);

  master.DeleteWorkflowResponse deleteWorkflow(1: master.DeleteWorkflowRequest req);

  master.CreateRunResponse createRun(1: master.CreateRunRequest req);

  master.GetRunResponse getRun(1: master.GetRunRequest req);

  master.ListRunsResponse listRuns(1: master.ListRunsRequest req);

  master.DeleteRunResponse deleteRun(1: master.DeleteRunRequest req);

  master.KillRunResponse killRun(1: master.KillRunRequest req);
}