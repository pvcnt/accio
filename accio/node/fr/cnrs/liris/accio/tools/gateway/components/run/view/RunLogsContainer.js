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

import xhr from '../../../utils/xhr';
import RunLogs from './RunLogs';

class RunLogsContainer extends React.Component {
  constructor(props) {
    super(props);
    this.state = { data: null, skip: 0 };
  }

  _loadData(props) {
    const url = `/api/v1/run/${props.runId}/logs/${props.nodeName}/${props.classifier}?skip=${this.state.skip}`;
    this.xhr = xhr(url).then(data => {
      this.setState((prevState) => {
        return {
          skip: prevState.skip + data.length,
          data: (null === prevState.data) ? data : prevState.data.concat(data),
        };
      });
    });
  }

  componentDidMount() {
    this._loadData(this.props);
    if (this.props.stream) {
      this.intervalId = setInterval(() => this._loadData(this.props), 10000);
    }
  }

  componentWillReceiveProps(nextProps) {
    if (this.intervalId) {
      clearInterval(this.intervalId);
      this.intervalId = null;
    }
    this._loadData(nextProps);
    if (props.stream) {
      this.intervalId = setInterval(() => this._loadData(this.props), 10000);
    }
  }

  componentWillUnmount() {
    if (this.intervalId) {
      clearInterval(this.intervalId);
      this.intervalId = null;
    }
    if (this.xhr) {
      this.xhr.cancel();
    }
  }

  render() {
    return <RunLogs logs={this.state.data}/>;
  }
}

RunLogsContainer.propTypes = {
  runId: React.PropTypes.string.isRequired,
  nodeName: React.PropTypes.string.isRequired,
  classifier: React.PropTypes.string.isRequired,
  stream: React.PropTypes.bool.isRequired,
};

export default RunLogsContainer;
