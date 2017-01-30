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
import RunArtifacts from './RunArtifacts'

class RunArtifactsContainer extends React.Component {
  constructor(props) {
    super(props)
    this.state = {artifacts: null, metrics: null}
  }

  _loadArtifacts(props) {
    if (this.props.showArtifacts) {
      this.artifactsXhr = xhr('/api/v1/run/' + props.runId + '/artifacts/' + props.nodeName)
        .then(artifacts => this.setState({artifacts}))
    } else {
      this.setState({artifacts: null})
    }
  }

  _loadMetrics(props) {
    if (this.props.showMetrics) {
      this.metricsXhr = xhr('/api/v1/run/' + props.runId + '/metrics/' + props.nodeName)
        .then(metrics => this.setState({metrics}))
    } else {
      this.setState({metrics: null})
    }
  }

  componentDidMount() {
    this._loadArtifacts(this.props)
    this._loadMetrics(this.props)
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.runId != this.props.runId || nextProps.nodeName != this.props.nodeName || nextProps.showArtifacts != this.props.showArtifacts) {
      this._loadArtifacts(nextProps)
    }
    if (nextProps.runId != this.props.runId || nextProps.nodeName != this.props.nodeName || nextProps.showMetrics != this.props.showMetrics) {
      this._loadMetrics(nextProps)
    }
  }

  componentWillUnmount() {
    if (this.artifactsXhr) {
      this.artifactsXhr.cancel()
    }
    if (this.metricsXhr) {
      this.metricsXhr.cancel()
    }
  }

  render() {
    return ((!this.props.showArtifacts || this.state.artifacts) && (!this.props.showMetrics || this.state.metrics))
      ? <RunArtifacts nodeName={this.props.nodeName} {...this.state}/>
      : <Spinner spinnerName="three-bounce"/>
  }
}

RunArtifactsContainer.propTypes = {
  runId: React.PropTypes.string.isRequired,
  nodeName: React.PropTypes.string.isRequired,
  showArtifacts: React.PropTypes.bool.isRequired,
  showMetrics: React.PropTypes.bool.isRequired,
}
RunArtifactsContainer.defaultProps = {
  showArtifacts: true,
  showMetrics: true,
}

export default RunArtifactsContainer
