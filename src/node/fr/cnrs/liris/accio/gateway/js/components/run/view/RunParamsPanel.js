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
import {Link} from "react-router"
import {Row, Col, Panel} from "react-bootstrap"
import {map} from 'lodash'
import {prettyPrintValue} from '../../../utils/prettyPrint'
import Username from '../../Username'

let RunParamsPanel = React.createClass({
  render: function () {
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
    );
  }
});

RunParamsPanel.propTypes = {
  run: React.PropTypes.object.isRequired,
};

export default RunParamsPanel;
