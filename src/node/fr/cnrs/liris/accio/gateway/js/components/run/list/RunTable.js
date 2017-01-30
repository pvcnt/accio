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
import moment from 'moment'
import {Link} from 'react-router'
import {ProgressBar, Table, Label, Glyphicon} from 'react-bootstrap'
import TagList from '../../TagList'

class RunTable extends React.Component {
  render() {
    const rows = this.props.runs.map((run, idx) => {
      const style = (run.state.completed_at) ? (run.state.status == 'success') ? 'success' : 'danger' : 'warning'
      const progress = Math.round(run.state.progress * 100)
      return <tr key={idx}>
        {this.props.showWorkflow ? <td><input type="checkbox" /></td> : null}
        <td>
          <Link to={'/runs/view/' + run.id}>
            {run.name ? run.name : 'Untitled run #' + run.id}
          </Link>
          {run.children.length ? <span style={{marginLeft: '10px'}}><Glyphicon glyph="tasks"/>&nbsp;{run.children.length}</span> : null}
        </td>
        {this.props.showWorkflow ? <td>{run.pkg.workflow_id}</td> : null}
        {this.props.showOwner ? <td>{run.owner.name}</td> : null}
        <td><ProgressBar now={progress} label={progress + '%'} bsStyle={style}/></td>
        <td>{(run.state.started_at) ? moment(run.state.started_at).fromNow() : '–'}</td>
        {this.props.showTags ? <td><TagList tags={run.tags}/></td> : null}
      </tr>
    });
    return <Table striped hover responsive className="accio-list-table">
      <thead>
      <tr>
        {this.props.showWorkflow ? <th>&nbsp;</th> : null}
        <th>Run name</th>
        {this.props.showWorkflow ? <th>Workflow</th> : null}
        {this.props.showOwner ? <th>Owner</th> : null}
        <th>Progress</th>
        <th>Started</th>
        {this.props.showTags ? <th>Tags</th> : null}
      </tr>
      </thead>
      <tbody>
      {rows}
      </tbody>
    </Table>
  }
}

RunTable.propTypes = {
  runs: React.PropTypes.array.isRequired,
  showWorkflow: React.PropTypes.bool.isRequired,
  showOwner: React.PropTypes.bool.isRequired,
  showTags: React.PropTypes.bool.isRequired,
}
RunTable.defaultProps = {
  showWorkflow: true,
  showOwner: true,
  showTags: true,
}

export default RunTable;
