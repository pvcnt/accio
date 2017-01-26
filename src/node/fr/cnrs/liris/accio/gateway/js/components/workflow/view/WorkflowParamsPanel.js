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

import React from 'react'
import {Row, Col, Panel, Glyphicon} from 'react-bootstrap'
import {prettyPrintValue, prettyPrintKind} from '../../../utils/prettyPrint'

class WorkflowParamsPanel extends React.Component {
  render() {
    const rows = this.props.workflow.params.map((param, idx) => {
      return <Row key={idx}>
        <Col md={2} className="accio-view-label">{param.name}</Col>
        <Col md={2}>{prettyPrintKind(param.kind)}</Col>
        <Col md={8}>
          {param.default_value
            ? prettyPrintValue(param.default_value.payload, param.kind)
            : param.is_optional ? <em>Optional</em> : <em>Required</em>}
        </Col>
      </Row>;
    });
    return <Panel header="Workflow parameters"
           className="accio-view-panel"
           collapsible={true}
           defaultExpanded={false}>
      {rows.length > 0 ? rows : <em>No workflow parameter.</em>}
    </Panel>
  }
}

WorkflowParamsPanel.propTypes = {
  workflow: React.PropTypes.object.isRequired
}

export default WorkflowParamsPanel
