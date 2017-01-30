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
import moment from "moment";
import {Row, Col, Badge, Glyphicon, Label, Modal, Panel, Button} from "react-bootstrap";
import {sortBy} from 'lodash'
import RunChildrenContainer from "./RunChildrenContainer";
import NodeStatusRow from './NodeStatusRow'

let RunStatusPanel = React.createClass({
  getInitialState: function () {
    return {
      error: null,
      logs: null,
    };
  },

  _handleErrorModalClose: function () {
    this.setState({error: null});
  },

  _handleErrorModalShow: function (node, error) {
    this.setState({error: {data: error, node: node}});
  },

  _handleLogsToggle: function (node, classifier) {
    if (this.state.logs && this.state.logs.node == node && this.state.logs.classifier == classifier) {
      this.setState({logs: null});
    } else {
      this.setState({logs: null}, () => this.setState({logs: {node, classifier}}));
    }
  },

  render: function () {
    const run = this.props.run;
    const isStarted = (run.state.started_at != null);
    const isCompleted = (run.state.completed_at != null);
    const isSuccessful = (run.state.status == 'success');

    const duration = isCompleted
      ? run.state.completed_at - run.state.started_at
      : isStarted
      ? moment().valueOf() - run.state.started_at
      : null
    const statusGlyph = isCompleted
      ? (isSuccessful ? 'ok' : 'remove')
      : isStarted
      ? 'refresh'
      : 'inbox'
    const statusText = isCompleted ? 'Successful' : isStarted ? 'Running' : 'Scheduled'

    const nodes = sortBy(run.state.nodes, ['started_at'])
    const nodeRows = nodes.map((node, idx) =>
      <NodeStatusRow key={idx}
                     runId={run.id}
                     node={node}
                     logs={this.state.logs && this.state.logs.node == node.name ? this.state.logs.classifier : null}
                     onLogsShow={this._handleLogsToggle}
                     onErrorShow={this._handleErrorModalShow}/>
    );

    const errorModal = (null !== this.state.error) ? (
      <Modal show={true} onHide={this._handleErrorModalClose} keyboard={true} bsSize="large">
        <Modal.Header closeButton>
          <Modal.Title>Exception for {this.state.error.node}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {this.state.error.data.root.classifier}: {this.state.error.data.root.message}
          <pre>{this.state.error.data.root.stacktrace.join("\n")}</pre>
          pre>
        </Modal.Body>
      </Modal>
    ) : null

    return (
      <Panel header={<h3><Glyphicon glyph={statusGlyph}/> Run status: {statusText}</h3>}
             className="accio-view-panel"
             collapsible={true}
             defaultExpanded={!isCompleted}>
        <Row>
          <Col sm={2} className="accio-view-label">Started</Col>
          <Col sm={10}>{moment(run.state.started_at).format('MMM Do YYYY, hh:mma')}</Col>
        </Row>
        <Row>
          <Col sm={2} className="accio-view-label">Completed</Col>
          <Col sm={10}>{run.state.completed_at ? moment(run.state.completed_at).format('MMM Do YYYY, hh:mma') : '–'}</Col>
        </Row>
        <Row>
          <Col sm={2} className="accio-view-label">Duration</Col>
          <Col sm={10}>{duration ? moment.duration(duration).humanize() : '–'}</Col>
        </Row>
        <Row>
          <Col sm={2} className="accio-view-label">{run.children ? 'Child runs' : 'Nodes'}</Col>
          <Col sm={10}>
            {run.children
              ? <RunChildrenContainer run={run}/>
              : nodeRows}
          </Col>
        </Row>
        {errorModal}
      </Panel>
    )
  }
});

RunStatusPanel.propTypes = {
  run: React.PropTypes.object.isRequired
};

export default RunStatusPanel;
