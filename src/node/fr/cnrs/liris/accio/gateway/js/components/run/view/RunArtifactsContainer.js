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

import React from 'react'
import Spinner from 'react-spinkit'
import xhr from '../../../utils/xhr'
import autobind from 'autobind-decorator'
import RunArtifacts from './RunArtifacts'

class RunArtifactsContainer extends React.Component {
  constructor(props) {
    super(props)
    this.state = {data: null}
  }

  @autobind
  _loadData(props) {
    xhr('/api/v1/run/' + props.runId + '/artifacts/' + props.nodeName)
      .then(data => this.setState({data}))
  }

  componentDidMount() {
    this._loadData(this.props)
  }

  componentWillReceiveProps(nextProps) {
    this._loadData(nextProps)
  }

  render() {
    return this.state.data
      ? <RunArtifacts nodeName={this.props.nodeName} artifacts={this.state.data}/>
      : <Spinner spinnerName="three-bounce"/>
  }
}

RunArtifactsContainer.propTypes = {
  runId: React.PropTypes.string.isRequired,
  nodeName: React.PropTypes.string.isRequired,
};

export default RunArtifactsContainer;
