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

import React from "react";
import moment from "moment";
import {noop} from "lodash";
import {Row, Col, Glyphicon, Label, Button} from "react-bootstrap";
import RunLogsContainer from "./RunLogsContainer";

let TaskStateRow = React.createClass({
  getDefaultProps: function() {
    return {
      onErrorShow: noop,
      onLogsShow: noop,
    };
  },

  _handleErrorModalShow: function (e) {
    e.nativeEvent.preventDefault();
    this.props.onErrorShow(this.props.node.name, this.props.node.result.error);
  },

  _handleLogsToggle: function (e, classifier) {
    e.nativeEvent.preventDefault();
    this.props.onLogsShow(this.props.node.name, classifier);
  },

  render: function () {
    const {node, runId} = this.props;
    const isStarted = (node.started_at != null);
    const isCompleted = (node.completed_at != null);
    const isSuccessful = (node.status == 'success');

    const duration = isCompleted
      ? node.completed_at - node.started_at
      : isStarted
      ? moment().valueOf() - node.started_at
      : null
    const glyph = isCompleted
      ? (isSuccessful ? 'ok' : node.status == 'lost' ? 'time' : node.status == 'killed' ? 'exclamation-sign' : 'remove')
      : isStarted
      ? 'refresh'
      : 'upload'
    const label = node.cache_hit
      ? <Label>Cache hit</Label>
      : node.status == 'cancelled'
      ? <Label bsStyle="danger">Cancelled</Label>
      : isCompleted
      ? <Label bsStyle={isSuccessful ? 'success' : 'danger'}>
        Ran for {moment.duration(duration).humanize()}
      </Label>
      : isStarted
      ? <Label bsStyle="info">
        Running for {moment.duration(duration).humanize()}
      </Label>
      : node.status === 'waiting'
      ? <Label>Waiting</Label>
      : <Label>Scheduled</Label>
    const hasLogs = !node.cache_hit && node.status !== 'waiting' && node.status !== 'scheduled'

    return (<div>
      <Row>
        <Col sm={5}>
          <Glyphicon glyph={glyph}/>&nbsp;{node.name}
        </Col>
        <Col sm={3}>{label}</Col>
        <Col sm={4}>
          {hasLogs
            ? <div>
                <Button
                bsSize="xsmall"
                onClick={e => this._handleLogsToggle(e, 'stdout')}
                bsStyle={this.props.logs == 'stdout' ? 'primary' : 'default'}>
                  stdout
              </Button>&nbsp;
              <Button
                bsSize="xsmall"
                onClick={e => this._handleLogsToggle(e, 'stderr')}
                bsStyle={this.props.logs == 'stderr' ? 'primary' : 'default'}>
                  stderr
              </Button>&nbsp;
              {this.props.logs
                ? <Button bsSize="xsmall" href={'/api/v1/run/' + runId + '/logs/' + node.name + '/' + this.props.logs + '?download=true'}>
                    <Glyphicon glyph="save"/>
                </Button> : null}
              </div>
            : null}
          {(node.result && node.result.error)
          ? <Button
              bsSize="xsmall"
              onClick={e => this._handleErrorModalShow(e)}
              bsStyle="danger">
              error
            </Button> : null}
        </Col>
      </Row>
      {(null != this.props.logs)
        ? <RunLogsContainer
            runId={runId}
            nodeName={node.name}
            classifier={this.props.logs}
            stream={!isCompleted}/>
        : null}
    </div>);
  }
});

TaskStateRow.propTypes = {
  runId: React.PropTypes.string.isRequired,
  node: React.PropTypes.object.isRequired,
  logs: React.PropTypes.string,
  onLogsShow: React.PropTypes.func.isRequired,
  onErrorShow: React.PropTypes.func.isRequired,
};

export default TaskStateRow;
