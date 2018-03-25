/*
 * Accio is a program whose purpose is to study location privacy.
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
import {Grid} from 'react-bootstrap';
import Spinner from 'react-spinkit';
import autobind from 'autobind-decorator';

import WorkflowEdit from './WorkflowEdit';
import xhr from '../../../utils/xhr';

class WorkflowEditContainer extends React.Component {
  constructor(props) {
    super(props);
    this.state = {workflow: null, operators: null};
  }

  _loadData(props) {
    xhr('/api/v1/workflow/' + props.params.id)
      .then(workflow => this.setState({ workflow }));
    xhr('/api/v1/operator')
      .then(data => this.setState({ operators: data.results }));
  }

  @autobind
  handleChange(workflow) {
    this.setState({ workflow });
  }

  componentWillReceiveProps(nextProps) {
    if (this.props.params.id !== nextProps.params.id) {
      this._loadData(nextProps);
    }
  }

  componentDidMount() {
    this._loadData(this.props);
  }

  render() {
    return (null !== this.state.workflow && null !== this.state.operators)
      ? <WorkflowEdit operators={this.state.operators}
                      workflow={this.state.workflow}
                      onChange={this.handleChange}/>
      : <Grid><Spinner spinnerName="three-bounce"/></Grid>;
  }
}

WorkflowEditContainer.propTypes = {
  params: React.PropTypes.shape({
    id: React.PropTypes.string.isRequired,
  })
};

export default WorkflowEditContainer;
