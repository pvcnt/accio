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
import moment from 'moment'
import {Col, Glyphicon, Panel, Row} from 'react-bootstrap'
import autobind from 'autobind-decorator'
import {sortBy} from 'lodash'
import ChildrenTableContainer from './ChildrenTableContainer'
import TaskStateRow from './TaskStateRow'

class StatusPanel extends React.Component {
  constructor(props) {
    super(props);
    this.state = {logs: null};
  }

  @autobind
  _handleLogsToggle(task, kind) {
    if (this.state.logs && this.state.logs.task === task && this.state.logs.kind === kind) {
      this.setState({logs: null});
    } else {
      this.setState({logs: null}, () => this.setState({logs: {task, kind}}));
    }
  }

  render() {
    const {job} = this.props;
    const isStarted = !!job.status.start_time;
    const isCompleted = !!job.status.end_time;
    const isSuccessful = job.status.state === 'successful';

    const duration = isCompleted
      ? job.status.start_time - job.status.end_time
      : isStarted
        ? moment().valueOf() - job.status.start_time
        : null;
    const statusGlyph = isCompleted
      ? (isSuccessful ? 'ok' : 'remove')
      : isStarted
        ? 'refresh'
        : 'upload';
    const statusText = isCompleted
      ? (isSuccessful ? 'Successful' : 'Failed')
      : isStarted
        ? 'Running'
        : 'Scheduled';

    const tasks = sortBy(job.status.tasks, ['start_time']);
    const rows = tasks.map((task, idx) =>
      <TaskStateRow key={idx}
                    job={job}
                    node={task}
                    logs={this.state.logs && this.state.logs.task === task.name ? this.state.logs.kind : null}
                    onLogsShow={this._handleLogsToggle}/>
    );

    return (
      <Panel header={<h3><Glyphicon glyph={statusGlyph}/> Run status: {statusText}</h3>}
             className="accio-view-panel"
             collapsible={true}
             defaultExpanded={!isCompleted}>
        <Row>
          <Col sm={2} className="accio-view-label">Started</Col>
          <Col
            sm={10}>{job.state.started_at ? moment(job.state.started_at).format('MMM Do YYYY, hh:mma') : '–'}</Col>
        </Row>
        <Row>
          <Col sm={2} className="accio-view-label">Completed</Col>
          <Col
            sm={10}>{job.state.completed_at ? moment(job.state.completed_at).format('MMM Do YYYY, hh:mma') : '–'}</Col>
        </Row>
        <Row>
          <Col sm={2} className="accio-view-label">Duration</Col>
          <Col sm={10}>{duration ? moment.duration(duration).humanize() : '–'}</Col>
        </Row>
        <Row>
          <Col sm={2}
               className="accio-view-label">{job.children.length ? 'Child jobs' : 'Nodes'}</Col>
          <Col sm={10}>
            {job.children.length
              ? <ChildrenTableContainer job={job}/>
              : nodeRows}
          </Col>
        </Row>
      </Panel>
    )
  }
}

StatusPanel.propTypes = {
  job: React.PropTypes.object.isRequired
};

export default StatusPanel;
