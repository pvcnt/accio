import React from "React";
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
          <img src="images/stack-32px.png"/> {this.props.workflow.name}
        </h2>

        <div className="accio-actions">
          <Button href={'/api/workflow/' + this.props.workflow.id + '?download=true'}>
            <Glyphicon glyph="save"/> Download as JSON
          </Button>

          <LinkContainer to={{path: '/runs/create', query: {workflow: this.props.workflow.id}}}>
            <Button><Glyphicon glyph="plus"/> Launch this workflow</Button>
          </LinkContainer>
        </div>

        <WorkflowDetailsPanel workflow={this.props.workflow}/>

        <WorkflowParamsPanel workflow={this.props.workflow}/>

        <LazyPanel header="Workflow graph"
                   className="accio-view-panel accio-view-panel-graph"
                   collapsible={true}
                   defaultExpanded={false}>
          <GraphView graph={this.props.workflow.graph} height={500}/>
        </LazyPanel>
      </Grid>
    );
  }
});

WorkflowView.propTypes = {
  workflow: React.PropTypes.object.isRequired
};

export default WorkflowView;
