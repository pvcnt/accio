/*
 * Accio is a program whose purpose is to study location privacy.
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

import React from 'react'
import {Row, Col} from 'react-bootstrap'
import {prettyPrintValue} from '../../../utils/prettyPrint'

class RawArtifactGroup extends React.Component {
  render() {
    const rows = this.props.artifacts.map((artifact, idx) => {
      return <Row key={idx}>
        <Col sm={3} className="accio-view-label">{artifact.name}</Col>
        <Col sm={9}>{prettyPrintValue(artifact.value.payload, artifact.value.kind)}</Col>
      </Row>
    });
    return <div>{rows}</div>
  }
}

RawArtifactGroup.propTypes = {
  artifacts: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
}

export default RawArtifactGroup;
