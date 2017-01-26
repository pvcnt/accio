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
import xhr from '../../../utils/xhr'
import RunTable from '../list/RunTable'
import Spinner from 'react-spinkit'
import autobind from 'autobind-decorator'

class RunChildrenContainer extends React.Component {
  constructor(props) {
    super(props)
    this.state = {data: null}
  }

  @autobind
  _loadData (props) {
    xhr('/api/v1/run?per_page=50&parent=' + props.run.id)
      .then(data => this.setState({data: data.results}))
  }

  componentDidMount() {
    this._loadData(this.props);
  }

  componentWillReceiveProps(nextProps) {
    this._loadData(nextProps)
  }

  render() {
    return (null != this.state.data)
      ? <RunTable runs={this.state.data} showWorkflow={false} showTags={false} showOwner={false}/>
      : <Spinner spinnerName="three-bounce"/>
  }
}
RunChildrenContainer.propTypes = {
  run: React.PropTypes.object.isRequired,
}

export default RunChildrenContainer