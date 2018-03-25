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
import React from "react";
import Spinner from "react-spinkit";
import xhr from "../../../utils/xhr";
import ChildrenTable from "./ChildrenTable";

class ChildrenTableContainer extends React.Component {
  constructor(props) {
    super(props)
    this.state = {data: null}
  }

  _loadData (props) {
    this.xhr = xhr('/api/v1/run?per_page=50&parent=' + props.run.id)
      .then(data => this.setState({data: data.results}))
  }

  componentDidMount() {
    this._loadData(this.props);
  }

  componentWillReceiveProps(nextProps) {
    this._loadData(nextProps)
  }

  componentWillUnmount() {
    if (this.xhr) {
      this.xhr.cancel()
    }
  }

  render() {
    return (null != this.state.data)
      ? <ChildrenTable runs={this.state.data}/>
      : <Spinner spinnerName="three-bounce"/>
  }
}
ChildrenTableContainer.propTypes = {
  run: React.PropTypes.object.isRequired,
}

export default ChildrenTableContainer
