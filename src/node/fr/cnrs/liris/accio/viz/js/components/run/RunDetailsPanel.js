import React from "React";
import {Link} from "react-router";
import {Row, Col, Panel} from "react-bootstrap";
import TagList from "../TagList";

let RunDetailsPanel = React.createClass({
  render: function () {
    const workflowLink = 'workflows/view/' + this.props.experiment.workflow.name;
    return (
      <Panel header="Run details"
             className="accio-view-panel"
             collapsible={true}
             defaultExpanded={true}>
        <Row>
          <Col sm={2} className="accio-view-label">Workflow</Col>
          <Col sm={10}>
            <Link to={workflowLink}>{this.props.experiment.workflow.name}</Link>
          </Col>
        </Row>
        <Row>
          <Col sm={2} className="accio-view-label">Owner</Col>
          <Col sm={10}>{this.props.experiment.owner}</Col>
        </Row>
        <Row>
          <Col sm={2} className="accio-view-label">Tags</Col>
          <Col sm={10}><TagList tags={this.props.experiment.tags}/></Col>
        </Row>
      </Panel>
    );
  }
});

RunDetailsPanel.propTypes = {
  run: React.PropTypes.object.isRequired,
  experiment: React.PropTypes.object.isRequired
};

export default RunDetailsPanel;
