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

import React from "react";
import {Row, Col, Panel} from "react-bootstrap";
import TagList from "../../TagList";

let RunMetadataPanel = React.createClass({
  render: function () {
    return (
      <Panel header="Run metadata"
             className="accio-view-panel"
             collapsible={true}
             defaultExpanded={true}>
        <Row>
          <Col sm={2} className="accio-view-label">Name</Col>
          <Col sm={10}>{this.props.run.name}</Col>
        </Row>
        <Row>
          <Col sm={2} className="accio-view-label">Notes</Col>
          <Col sm={10}>{this.props.run.notes}</Col>
        </Row>
        <Row>
          <Col sm={2} className="accio-view-label">Tags</Col>
          <Col sm={10}><TagList tags={this.props.run.tags}/></Col>
        </Row>
      </Panel>
    );
  }
});

RunMetadataPanel.propTypes = {
  run: React.PropTypes.object.isRequired,
};

export default RunMetadataPanel;
