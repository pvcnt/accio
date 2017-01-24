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
import {LinkContainer} from "react-router-bootstrap";
import {Grid, Button, Glyphicon} from "react-bootstrap";
import GraphView from "../GraphView";
import LazyPanel from "../LazyPanel";
import WorkflowDetailsPanel from "./WorkflowDetailsPanel";
import WorkflowParamsPanel from "./WorkflowParamsPanel";

let WorkflowView = React.createClass({
  render: function () {
    return (
      <Grid>
        <h2 className="accio-title">
          <img src="images/stack-32px.png"/> {this.props.workflow.id.value}
        </h2>

        <div className="accio-actions">
          <Button href={'/api/v1/workflow/' + this.props.workflow.id.value + '?download=true'}>
            <Glyphicon glyph="save"/> Download as JSON
          </Button>
        </div>

        <WorkflowDetailsPanel workflow={this.props.workflow}/>

        <WorkflowParamsPanel workflow={this.props.workflow}/>

        <LazyPanel header="Workflow graph"
                   className="accio-view-panel accio-view-panel-graph"
                   collapsible={true}
                   defaultExpanded={false}>
          <GraphView graph={this.props.workflow.graph.nodes} height={500}/>
        </LazyPanel>
      </Grid>
    );
  }
});

WorkflowView.propTypes = {
  workflow: React.PropTypes.object.isRequired
};

export default WorkflowView;
