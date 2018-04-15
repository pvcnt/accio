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

import React from "react";
import moment from "moment";
import autobind from 'autobind-decorator';
import {noop} from "lodash";
import {Button, Col, Glyphicon, Label, Row} from "react-bootstrap";
import RunLogsContainer from "./RunLogsContainer";

class TaskStateRow extends React.Component {
  @autobind
  _handleLogsToggle(e, classifier) {
    e.nativeEvent.preventDefault();
    this.props.onLogsShow(this.props.task.name, classifier);
  }

  render() {
    const {task, job} = this.props;
    const isStarted = !!task.start_time;
    const isCompleted = !!task.end_time;
    const isSuccessful = task.state === 'successful';

    const duration = isCompleted
      ? task.end_time - task.start_time
      : isStarted
        ? moment().valueOf() - task.start_time
        : null;
    const glyph = isCompleted
      ? (isSuccessful ? 'ok' : task.state === 'killed' ? 'exclamation-sign' : 'remove')
      : isStarted
        ? 'refresh'
        : 'upload';
    const label = task.state === 'cancelled'
      ? <Label bsStyle="danger">Cancelled</Label>
      : isCompleted
        ? <Label bsStyle={isSuccessful ? 'success' : 'danger'}>
          Ran for {moment.duration(duration).humanize()}
        </Label>
        : isStarted
          ? <Label bsStyle="info">
            Running for {moment.duration(duration).humanize()}
          </Label>
          : task.state === 'waiting'
            ? <Label>Waiting</Label>
            : <Label>Scheduled</Label>;
    const hasLogs = task.state !== 'waiting' && task.state !== 'scheduled';

    return (
      <div>
        <Row>
          <Col sm={5}>
            <Glyphicon glyph={glyph}/>&nbsp;{task.name}
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
                  ? <Button bsSize="xsmall"
                            href={'/api/v1/run/' + runId + '/logs/' + task.name + '/' + this.props.logs + '?download=true'}>
                    <Glyphicon glyph="save"/>
                  </Button> : null}
              </div>
              : null}
          </Col>
        </Row>
        {(null != this.props.logs)
          ? <RunLogsContainer
            runId={runId}
            taskName={task.name}
            classifier={this.props.logs}
            stream={!isCompleted}/>
          : null}
      </div>
    );
  }
}

TaskStateRow.propTypes = {
  job: React.PropTypes.object.isRequired,
  task: React.PropTypes.object.isRequired,
  logs: React.PropTypes.string,
  onLogsShow: React.PropTypes.func.isRequired,
};

export default TaskStateRow;
