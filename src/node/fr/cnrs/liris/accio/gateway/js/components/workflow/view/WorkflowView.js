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
import {Grid, Button, Glyphicon} from 'react-bootstrap'
import GraphView from '../GraphView'
import LazyPanel from '../../LazyPanel'
import WorkflowDetailsPanel from './WorkflowDetailsPanel'
import WorkflowParamsPanel from './WorkflowParamsPanel'

class WorkflowView extends React.Component {
  render() {
    return (
      <Grid>
        <h2 className="accio-title">
          <img src="images/stack-32px.png"/> {this.props.workflow.id}:{this.props.workflow.version}
        </h2>

        <WorkflowDetailsPanel workflow={this.props.workflow}/>
        <WorkflowParamsPanel workflow={this.props.workflow}/>
        <LazyPanel header="Operators graph"
                   className="accio-view-panel accio-view-panel-graph"
                   collapsible={true}
                   defaultExpanded={false}>
          <GraphView graph={this.props.workflow.graph} height={500}/>
        </LazyPanel>
      </Grid>
    )
  }
}

WorkflowView.propTypes = {
  workflow: React.PropTypes.object.isRequired,
  lastRuns: React.PropTypes.array,
}

export default WorkflowView
