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
import autobind from 'autobind-decorator';
import { NonIdealState, Spinner } from '@blueprintjs/core';
import xhr from '../../utils/xhr';

export default function withJob(WrappedComponent) {
  return class WithJobContainer extends React.Component {
    constructor(props) {
      super(props);
      this.state = {
        isLoading: false,
        isLoaded: false,
        data: null,
      };
    }

    @autobind
    onSuccess(resp) {
      this.setState({ isLoading: false, isLoaded: true, data: resp });
    }

    @autobind
    onError(resp) {
      console.log('Unexpected error while fetching job', resp);
      this.setState({ isLoading: false, isLoaded: true });
    }

    load(props) {
      xhr(`/api/jobs/${props.match.params.name}`).then(this.onSuccess, this.onError);
    }

    componentDidMount() {
      this.load(this.props);
    }

    componentWillReceiveProps(nextProps) {
      this.load(nextProps);
    }

    render() {
      if (this.state.isLoading) {
        return <Spinner/>;
      } else if (this.state.isLoaded && null !== this.state.data) {
        return <WrappedComponent campaign={this.state.data}/>;
      } else if (this.state.isLoaded) {
        return <NonIdealState visual="error" title="An error occurred while loading job."/>;
      }
      return null;
    }
  }
}