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
import { getJob } from '../../actions';

const mapStateToProps = (state, ownProps) => {
  const name = getJobName(ownProps);
  return {
    isLoading: state.jobs.status[name] === 'loading',
    isLoaded: state.jobs.status[name] === 'loaded',
    isFailed: state.jobs.status[name] === 'failed',
    job: state.jobs.entities[name],
  };
};

function getJobName(props) {
  return props.match.params.name;
}

export default function withJob(WrappedComponent) {
  class WithJob extends React.Component {
    loadData() {
      this.props.dispatch(getJob(getJobName(this.props)));
    }

    componentDidMount() {
      this.loadData();
    }

    componentDidUpdate(prevProps) {
      if (getJobName(prevProps) !== getJobName(this.props)) {
        this.loadData();
      }
    }

    render() {
      if (this.props.isLoading) {
        return <Spinner/>;
      } else if (this.props.isFailed) {
        return <NonIdealState visual="error" title="An error occurred while loading job."/>;
      } else if (this.props.isLoaded) {
        return <WrappedComponent job={this.props.job}/>;
      }
      return null;
    }
  }

  return connect(mapStateToProps)(WithJob);
}