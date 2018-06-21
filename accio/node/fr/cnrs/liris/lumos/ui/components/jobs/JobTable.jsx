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

import React from 'react';
import PropTypes from 'prop-types';
import { withRouter } from 'react-router-dom';
import moment from 'moment';

@withRouter
class JobTable extends React.Component {
  handleClick(job) {
    this.props.history.push(`/jobs/view/${job.name}`);
  }

  render() {
    const rows = this.props.jobs.map((item, idx) => {
      return (
        <tr onClick={() => this.handleClick(item)} key={idx}>
          <td>{item.name}</td>
          <td>{item.status.state}</td>
        </tr>
      );
    });
    return (
      <table className="pt-html-table pt-interactive pt-html-table-striped"
             style={{ width: '100%' }}>
        <thead>
        <tr>
          <th>Name</th>
          <th>State</th>
        </tr>
        </thead>
        <tbody>{rows}</tbody>
      </table>
    );
  }
}

JobTable.propTypes = {
  jobs: PropTypes.array.isRequired,
  totalCount: PropTypes.number.isRequired,
  onPageChange: PropTypes.func.isRequired,
};

JobTable.defaultProps = {
  jobs: [],
  totalCount: 0,
};

export default JobTable;