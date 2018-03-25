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

import React from 'react'
import {Grid} from 'react-bootstrap'
import autobind from 'autobind-decorator'
import Spinner from 'react-spinkit'
import {isEqual} from 'lodash'
import xhr from '../../../utils/xhr'
import RunView from './RunView'

class RunViewContainer extends React.Component {
  constructor(props) {
    super(props)
    this.state = {data: null}
  }

  _loadData(props) {
    this.xhr = xhr('/api/v1/run/' + props.params.id).then(data => {
      if (data.parent) {
        this.xhr = xhr('/api/v1/run/' + data.parent).then(data2 => {
          data.parent = data2
          this.setState({data})
        })
      } else {
        this.setState({data})
      }
    })
  }

  componentWillUnmount() {
    if (this.xhr) {
      this.xhr.cancel()
    }
  }

  @autobind
  _handleChange(newRun) {
    this.setState({data: newRun})
  }

  componentWillReceiveProps(nextProps) {
    if (this.props.params.id !== nextProps.params.id) {
      this._loadData(nextProps)
    }
  }

  shouldComponentUpdate(nextProps, nextState) {
    return !isEqual(this.state.data, nextState.data)
  }

  componentDidMount() {
    this._loadData(this.props)
  }

  render() {
    return (null !== this.state.data)
      ? <RunView run={this.state.data} onChange={this._handleChange}/>
      : <Grid><Spinner spinnerName="three-bounce"/></Grid>
  }
}

RunViewContainer.propTypes = {
  params: React.PropTypes.shape({
    id: React.PropTypes.string.isRequired,
  })
}

export default RunViewContainer
