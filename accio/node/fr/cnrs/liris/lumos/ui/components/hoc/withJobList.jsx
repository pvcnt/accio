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
import { Spinner, NonIdealState } from '@blueprintjs/core';
import xhr from '../../utils/xhr';

export default function withJobList(WrappedComponent) {
  return class JobListContainer extends React.Component {
    constructor(props) {
      super(props);
      this.state = {
        isLoading: false,
        isLoaded: false,
        data: null,
      };
    }

    onSuccess(resp) {
      this.setState({ isLoading: false, isLoaded: true, data: resp });
    }

    onError(resp) {
      console.log('Unexpected error while fetching jobs.', resp);
      this.setState({ isLoading: false, isLoaded: true });
    }

    load(props) {
      this.setState({ isLoading: true });
      let url = '/api/v1/jobs';
      if (props.filter && Object.keys(props.filter).length > 0) {
        url += '?' + map(props.filter, (v, k) => `${k}=${encodeURIComponent(v)}`).join('&');
      }
      xhr(url).then(resp => this.onSuccess(resp), resp => this.onError(resp))
    }

    componentDidMount() {
      this.load(this.props);
    }

    componentWillReceiveProps(nextProps) {
      // We always reload the list of campaigns, even if the properties did not change, to avoid
      // showing stale data.
      this.load(nextProps);
    }

    render() {
      if (this.state.isLoading) {
        return <Spinner/>;
      } else if (this.state.isLoaded && null !== this.state.data) {
        return <WrappedComponent {...this.state.data}/>;
      } else if (this.state.isLoaded) {
        return <NonIdealState visual="error" title="An error occurred while loading jobs."/>;
      }
      return null;
    }
  };
}