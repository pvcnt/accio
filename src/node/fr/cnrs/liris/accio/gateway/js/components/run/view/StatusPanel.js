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
import moment from 'moment'
import {Row, Col, Glyphicon, Panel} from 'react-bootstrap'
import autobind from 'autobind-decorator'
import {sortBy} from 'lodash'
import ChildrenTableContainer from './ChildrenTableContainer'
import TaskStateRow from './TaskStateRow'
import ErrorModal from './ErrorModal'

class StatusPanel extends React.Component {
  constructor(props) {
    super(props)
    this.state = {error: null, logs: null}
  }

  @autobind
  _handleErrorModalClose() {
    this.setState({error: null})
  }

  @autobind
  _handleErrorModalShow(node, error) {
    this.setState({error: {data: error, node: node}})
  }

  @autobind
  _handleLogsToggle(node, classifier) {
    if (this.state.logs && this.state.logs.node == node && this.state.logs.classifier == classifier) {
      this.setState({logs: null})
    } else {
      this.setState({logs: null}, () => this.setState({logs: {node, classifier}}))
    }
  }

  render() {
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
      : 'upload'
    const statusText = isCompleted
      ? (isSuccessful ? 'Successful' : 'Failed')
      : isStarted
      ? 'Running'
      : 'Scheduled'

    const nodes = sortBy(run.state.nodes, ['started_at'])
    const nodeRows = nodes.map((node, idx) =>
      <TaskStateRow key={idx}
                     runId={run.id}
                     node={node}
                     logs={this.state.logs && this.state.logs.node == node.name ? this.state.logs.classifier : null}
                     onLogsShow={this._handleLogsToggle}
                     onErrorShow={this._handleErrorModalShow}/>
    )

    return (
      <Panel header={<h3><Glyphicon glyph={statusGlyph}/> Run status: {statusText}</h3>}
             className="accio-view-panel"
             collapsible={true}
             defaultExpanded={!isCompleted}>
        <Row>
          <Col sm={2} className="accio-view-label">Started</Col>
          <Col sm={10}>{run.state.started_at ? moment(run.state.started_at).format('MMM Do YYYY, hh:mma') : '–'}</Col>
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
          <Col sm={2} className="accio-view-label">{run.children.length ? 'Child runs' : 'Nodes'}</Col>
          <Col sm={10}>
            {run.children.length
              ? <ChildrenTableContainer run={run}/>
              : nodeRows}
          </Col>
        </Row>
        {(null !== this.state.error)
          ? <ErrorModal
              nodeName={this.state.error.node}
              error={this.state.error.data}
              onClose={this._handleErrorModalClose}/>
          : null}
      </Panel>
    )
  }
}

StatusPanel.propTypes = {
  run: React.PropTypes.object.isRequired
}

export default StatusPanel
