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
import { Spinner, NonIdealState } from '@blueprintjs/core';
import { connect } from 'react-redux';
import JobList from './JobList';
import { fetchJobs } from '../../actions';

const mapStateToProps = (state) => {
  const jobs = state.jobs.list.ids.map(name => state.jobs.entities[name]);
  return {
    jobs,
    page: state.jobs.list.page,
    filter: state.jobs.list.labelSelector || '',
    totalCount: state.jobs.list.totalCount,
    status: state.jobs.list.status,
    isLoading: state.jobs.list.status === 'loading',
    isLoaded: state.jobs.list.status === 'loaded',
    isFailed: state.jobs.list.status === 'failed',
  };
};

const mapDispatchToProps = (dispatch, ownProps) => {
  return {
    loadData: () => dispatch(fetchJobs()),
    onPageChange: (page) => dispatch(fetchJobs(page)),
    onFilterChange: (filter) => dispatch(fetchJobs(ownProps.page, filter || null)),
  }
};


@connect(mapStateToProps, mapDispatchToProps)
class JobListContainer extends React.Component {
  componentDidMount() {
    this.props.loadData();
  }

  render() {
    if (this.props.isLoading) {
      return <Spinner/>;
    } else if (this.props.isFailed) {
      return <NonIdealState visual="error" title="An error occurred while loading jobs."/>;
    } else if (this.props.isLoaded) {
      return <JobList {...this.props}/>;
    }
    return null;
  }
}

export default JobListContainer;