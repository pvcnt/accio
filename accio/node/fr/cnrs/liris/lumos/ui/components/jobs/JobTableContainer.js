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
import JobTable from './JobTable';
import { fetchJobs } from '../../actions';
//import withJobList from '../hoc/withJobList';

//export default withJobList(JobTable);


const mapStateToProps = (state, ownProps) => {
  const jobs = [];
  state.jobList.entities.forEach(name => {
    if (state.jobs.status[name] === 'loaded') {
      jobs.push(state.jobs.entities[name]);
    }
  });
  return {
    jobs,
    totalCount: state.jobList.totalCount,
    status: state.jobList.status,
    isLoading: state.jobList.status === 'loading',
    isLoaded: state.jobList.status === 'loaded',
    isFailed: state.jobList.status === 'failed',
  };
};

@connect(mapStateToProps)
export default class JobTableContainer extends React.Component {
  componentDidMount() {
    this.props.dispatch(fetchJobs());
  }

  render() {
    if (this.props.isLoading) {
      return <Spinner/>;
    } else if (this.props.isFailed) {
      return <NonIdealState visual="error" title="An error occurred while loading jobs."/>;
    } else if (this.props.isLoaded) {
      return <JobTable {...this.props}/>;
    }
    return null;
  }
}