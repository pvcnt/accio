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

import React from 'react';
import {Grid} from 'react-bootstrap';
import WorkflowEditor from '../editor/WorkflowEditor';

class WorkflowEdit extends React.Component {
  render() {
    return (
      <Grid>
        <h2 className="accio-title">
          <img src="images/stack-32px.png" /> {this.props.workflow.id}:{this.props.workflow.version}
        </h2>

        <WorkflowEditor workflow={this.props.workflow} />
      </Grid>
    );
  }
}

WorkflowEdit.propTypes = {
  workflow: React.PropTypes.object.isRequired,
};

export default WorkflowEdit;
