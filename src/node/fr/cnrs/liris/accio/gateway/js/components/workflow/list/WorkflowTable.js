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
import {Table} from 'react-bootstrap'

class WorkflowTable extends React.Component {
  render() {
    const rows = this.props.workflows.map((workflow, idx) => {
      return (
        <tr key={idx}>
          <td>
            <Link to={'/workflows/view/' + workflow.id}>
              {workflow.id}
            </Link>
          </td>
          <td>{workflow.name ? workflow.name : '&gt;no name&lt;'}</td>
          <td>{workflow.owner.name}</td>
          <td>{moment(workflow.created_at).fromNow()}</td>
        </tr>
      )
    })
    return (
      <Table striped hover responsive className="accio-list-table">
        <thead>
        <tr>
          <th>Workflow</th>
          <th>Name</th>
          <th>Owner</th>
          <th>Updated</th>
        </tr>
        </thead>
        <tbody>{rows}</tbody>
      </Table>
    )
  }
}

WorkflowTable.propTypes = {
  workflows: React.PropTypes.array.isRequired,
}

export default WorkflowTable