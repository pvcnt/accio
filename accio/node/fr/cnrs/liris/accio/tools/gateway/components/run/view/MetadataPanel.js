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

import React from 'react'
import {Col, Panel, Row} from 'react-bootstrap'
import TagList from '../../TagList'

class MetadataPanel extends React.Component {
  render() {
    const {job} = this.props;
    return (
      <div>
        <Panel header="Run metadata"
               className="accio-view-panel"
               collapsible={true}
               defaultExpanded={false}>
          <Row>
            <Col sm={2} className="accio-view-label">Name</Col>
            <Col sm={10}>{job.name}</Col>
          </Row>
          <Row>
            <Col sm={2} className="accio-view-label">Tags</Col>
            <Col sm={10}><TagList tags={job.tags}/></Col>
          </Row>
        </Panel>
      </div>
    )
  }
}

MetadataPanel.propTypes = {
  job: React.PropTypes.object.isRequired,
};

export default MetadataPanel;
