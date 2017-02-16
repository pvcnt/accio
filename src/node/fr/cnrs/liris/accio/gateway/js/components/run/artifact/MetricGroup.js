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
import {Row, Col, Glyphicon} from 'react-bootstrap'

class MetricGroup extends React.Component {
  render() {
    const rows = this.props.metrics.map((metric, idx) => {
      return <Row key={idx}>
        <Col sm={3} className="accio-view-label">
          <Glyphicon glyph="dashboard"/>&nbsp;{metric.name}
        </Col>
        <Col sm={9}>{metric.value}</Col>
      </Row>
    });
    return <div>{rows}</div>
  }
}

MetricGroup.propTypes = {
  metrics: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
}

export default MetricGroup;
