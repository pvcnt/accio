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
import JobTable from './JobTable';
import Pager from './Pager';
import JobSearch from './JobSearch';

class JobList extends React.Component {
  render() {
    return (
      <div>
        <JobSearch value={this.props.filter} onChange={this.props.onFilterChange}/>

        <JobTable jobs={this.props.jobs}/>

        <Pager totalCount={this.props.totalCount}
               onChange={this.props.onPageChange}
               page={this.props.page}/>
      </div>
    );
  }
}

JobTable.propTypes = {
  jobs: PropTypes.array.isRequired,
  page: PropTypes.number.isRequired,
  filter: PropTypes.string.isRequired,
  totalCount: PropTypes.number.isRequired,
  onPageChange: PropTypes.func.isRequired,
  onFilterChange: PropTypes.func.isRequired,
};

export default JobList;