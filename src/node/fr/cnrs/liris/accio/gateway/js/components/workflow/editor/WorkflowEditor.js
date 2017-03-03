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
import {Grid, Col} from "react-bootstrap";
import autobind from 'autobind-decorator';
import InteractiveGraph from './InteractiveGraph';
import Sidebar from './Sidebar';

class WorkflowEditor extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      selected: null,
    };
  }

  @autobind
  onChange(node) {
    this.setState({selected: node});
  }

   render() {
    return (
      <div className="workflow-editor">
        <Grid>
          <Col sm={9}>
            <InteractiveGraph workflow={this.props.workflow} onChange={this.onChange}/>
          </Col>
          <Col sm={3}>
            <Sidebar workflow={this.props.workflow} node={this.state.selected}/>
          </Col>
        </Grid>
      </div>
    );
  }
}

WorkflowEditor.propTypes = {
  workflow: React.PropTypes.object.isRequired,
};

export default WorkflowEditor;
