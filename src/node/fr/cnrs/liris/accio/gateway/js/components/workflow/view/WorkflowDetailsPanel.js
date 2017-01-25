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

import React from "react"
import moment from "moment"
import {Row, Col, Panel} from "react-bootstrap"
import Username from '../../Username'

let WorkflowDetailsPanel = React.createClass({
  render: function () {
    const workflow = this.props.workflow;
    return (
      <Panel header="Workflow details"
             className="accio-view-panel"
             collapsible={true}
             defaultExpanded={true}>
        <Row>
          <Col md={2} className="accio-view-label">Name</Col>
          <Col md={10}>{workflow.name ? workflow.name : '&gt;no name&lt;'}</Col>
        </Row>
        <Row>
          <Col md={2} className="accio-view-label">Created</Col>
          <Col md={10}>{moment(workflow.created_at).format('MMM Do YYYY, hh:mma')}</Col>
        </Row>
        <Row>
          <Col md={2} className="accio-view-label">Owner</Col>
          <Col md={10}><Username user={workflow.owner}/></Col>
        </Row>
      </Panel>
    );
  }
});

WorkflowDetailsPanel.propTypes = {
  workflow: React.PropTypes.object.isRequired
};

export default WorkflowDetailsPanel;
