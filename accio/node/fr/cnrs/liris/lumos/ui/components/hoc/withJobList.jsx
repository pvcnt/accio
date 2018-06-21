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
import { fetchJobs } from '../../actions';

const mapStateToProps = (state, ownProps) => {
  const jobs = state.jobs.list.ids.map(name => state.jobs.entities[name]);
  return {
    jobs,
    totalCount: state.jobs.list.totalCount,
    status: state.jobs.list.status,
    isLoading: state.jobs.list.status === 'loading',
    isLoaded: state.jobs.list.status === 'loaded',
    isFailed: state.jobs.list.status === 'failed',
  };
};

export default function withJobList(WrappedComponent) {
  class JobListContainer extends React.Component {
    constructor(props) {
      super(props);
      this.state = {
        page: 1,
      };
    }

    handlePageChange(page) {
      this.setState({ page }, newState => this.props.dispatch(fetchJobs(newState.page)));
    }

    loadData() {
      this.props.dispatch(fetchJobs(this.state.page));
    }

    componentDidMount() {
      this.loadData();
    }

    render() {
      if (this.props.isLoading) {
        return <Spinner/>;
      } else if (this.props.isFailed) {
        return <NonIdealState visual="error" title="An error occurred while loading jobs."/>;
      } else if (this.props.isLoaded) {
        return <WrappedComponent {...this.props}
                         onPageChange={page => this.handlePageChange(page)}/>;
      }
      return null;
    }
  }

  return connect(mapStateToProps)(JobListContainer);
}