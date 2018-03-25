/*
 * Accio is a platform to launch computer science experiments.
 * Copyright (C) 2016-2018 Vincent Primault <v.primault@ucl.ac.uk>
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
import {Row, Col, Panel} from "react-bootstrap"
import {map} from 'lodash'
import {prettyPrintValue} from '../../../utils/prettyPrint'

class ParamsPanel extends React.Component {
  render() {
    const rows = map(this.props.run.params, (value, name) => {
      return (
        <Row key={name}>
          <Col sm={2} className="accio-view-label">{name}</Col>
          <Col sm={10}>{prettyPrintValue(value.payload, value.kind)}</Col>
        </Row>
      )
    })
    return (
      <Panel header="Run parameters"
             className="accio-view-panel"
             collapsible={true}
             defaultExpanded={true}>
        {rows}
      </Panel>
    )
  }
}

ParamsPanel.propTypes = {
  run: React.PropTypes.object.isRequired,
}

export default ParamsPanel
