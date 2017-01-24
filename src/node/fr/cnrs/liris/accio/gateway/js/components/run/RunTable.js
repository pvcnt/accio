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
import {Link} from "react-router";
import {ProgressBar, Table, Label} from "react-bootstrap";

let RunTable = React.createClass({
  render: function () {
    const rows = this.props.runs.map((run, idx) => {
      const style = (run.state.completed_at) ? (run.state.status == 2) ? 'success' : 'danger' : 'warning';
      const tags = (run.tags.length > 0) ? run.tags.map((tag, idx) => <Label key={idx}>tag</Label>) : '–';
      const progress = Math.round(run.state.progress * 100)
      return (
        <tr key={idx}>
          <td><input type="checkbox" /></td>
          <td>
            <Link to={'/runs/view/' + run.id.value}>{run.name ? run.name : 'Untitled run #' + run.id.value}</Link>
          </td>
          <td>{run.pkg.workflow_id.value}</td>
          <td>{run.owner.name}</td>
          <td><ProgressBar now={progress} label={progress + '%'} bsStyle={style}/></td>
          <td>{(run.state.started_at) ? moment(run.state.started_at).fromNow() : '–'}</td>
          <td>{tags}</td>
        </tr>
      );
    });
    return (
      <Table striped hover responsive className="accio-list-table">
        <thead>
        <tr>
          <th></th>
          <th>Run name</th>
          <th>Workflow</th>
          <th>Owner</th>
          <th>Progress</th>
          <th>Started</th>
          <th>Tags</th>
        </tr>
        </thead>
        <tbody>
        {rows}
        </tbody>
      </Table>
    );
  }
});

RunTable.propTypes = {
  runs: React.PropTypes.array.isRequired,
};

export default RunTable;
