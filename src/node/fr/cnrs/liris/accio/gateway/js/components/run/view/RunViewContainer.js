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

import React from "react";
import {Grid} from "react-bootstrap";
import RunView from "./RunView";
import xhr from "../../../utils/xhr";
import Spinner from "react-spinkit";

let RunViewContainer = React.createClass({
  getInitialState: function () {
    return {
      data: null,
    };
  },

  _loadData: function (props) {
    xhr('/api/v1/run/' + props.params.id).then(data => this.setState({data}));
  },

  componentWillReceiveProps: function (nextProps) {
    if (this.props.params.id !== nextProps.params.id) {
      this._loadData(nextProps);
    }
  },

  componentDidMount: function () {
    this._loadData(this.props);
  },

  render: function () {
    return (null !== this.state.data)
      ? <RunView run={this.state.data} {...this.props}/>
      : <Grid><Spinner spinnerName="three-bounce"/></Grid>;
  }
});

RunViewContainer.propTypes = {
  params: React.PropTypes.shape({
    id: React.PropTypes.string.isRequired,
  })
};

export default RunViewContainer;
