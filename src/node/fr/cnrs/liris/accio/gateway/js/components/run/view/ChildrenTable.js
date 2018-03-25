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
import {Link} from 'react-router'
import {find} from 'lodash'
import {ProgressBar, Row, Col, Glyphicon} from 'react-bootstrap'

class ChildrenTable extends React.Component {
  render() {
    const rows = this.props.runs.map((run, idx) => {
      const style = (run.state.completed_at) ? (run.state.status == 'success') ? 'success' : 'danger' : 'warning'
      const progress = Math.round(run.state.progress * 100)
      let label = null
      if (run.state.status === 'running') {
        const activeNodes = run.state.nodes.filter(node => node.status === 'running');
        if (activeNodes.length > 0) {
          label = 'Running ' + activeNodes.map(node => node.name).join(', ')
        } else {
          const waitingNode = find(run.state.nodes, node => node.status === 'scheduled')
          if (waitingNode) {
            label = 'Waiting for ' + waitingNode.name
          }
        }
      } else if (run.state.status === 'killed') {
        label = 'Cancelled'
      } else if (run.state.status === 'failed') {
        const failedNode = find(run.state.nodes, node => node.status === 'failed')
        if (failedNode) {
          label = 'Failed in ' + failedNode.name
        } else {
          const lostNode = find(run.state.nodes, node => node.status === 'lost')
          if (lostNode) {
            label = 'Lost ' + lostNode.name
          }
        }
      }
      const startedAt = run.state.started_at
        ? <span><Glyphicon glyph="play"/>&nbsp;{moment(run.state.started_at).format('MMM D, HH:mm')}</span>
        : <span><Glyphicon glyph="upload"/>&nbsp;{moment(run.created_at).format('MMM D, HH:mm')}</span>
      const stoppedAt = run.state.completed_at
        ? <span><Glyphicon glyph="stop"/>&nbsp;{moment(run.state.completed_at).format('MMM D, HH:mm')}</span>
        : run.state.started_at
        ? <span><Glyphicon glyph="hourglass"/>&nbsp;{moment.duration(moment().valueOf() - run.state.started_at).humanize()}</span>
        : null
      return <Row key={idx} className="run-item">
        <Col md={2}>{startedAt}<br/>{stoppedAt}</Col>
        <Col md={6}>
          <Link to={'/runs/view/' + run.id}>{run.name ? run.name : 'Untitled run #' + run.id}</Link><br/>
          <span className="run-item-status">{label}</span>
        </Col>
        <Col md={4}><ProgressBar now={progress} label={progress + '%'} bsStyle={style}/></Col>
      </Row>
    })
    return <div className="run-list">{rows}</div>
  }
}

ChildrenTable.propTypes = {
  runs: React.PropTypes.array.isRequired,
}

export default ChildrenTable;
