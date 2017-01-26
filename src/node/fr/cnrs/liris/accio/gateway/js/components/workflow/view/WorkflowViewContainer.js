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
import WorkflowView from "./WorkflowView";
import xhr from "../../../utils/xhr";
import Spinner from "react-spinkit";

let WorkflowViewContainer = React.createClass({
  getInitialState: function () {
    return {
      workflow: null,
      lastRuns: null,
    };
  },

  _loadData: function (props) {
    const url = '/api/v1/workflow/' + props.params.id + (props.params.version ? '?version=' + props.params.version : '');
    xhr(url).then(data => this.setState({workflow: data}));

    xhr('/api/v1/run?per_page=15&workflow=' + props.params.id).then(data => this.setState({lastRuns: data.results}));
  },

  componentWillReceiveProps: function (nextProps) {
    if (this.props.params.id !== nextProps.params.id || this.props.params.version !== nextProps.params.version) {
      this._loadData(nextProps);
    }
  },

  componentDidMount: function () {
    this._loadData(this.props);
  },

  render: function () {
    return (null !== this.state.workflow)
      ? <WorkflowView {...this.state} {...this.props}/>
      : <Grid><Spinner spinnerName="three-bounce"/></Grid>;
  }
});

WorkflowViewContainer.propTypes = {
  params: React.PropTypes.shape({
    id: React.PropTypes.string.isRequired,
    version: React.PropTypes.string,
  })
};

export default WorkflowViewContainer;
