import React from "React";
import {Link} from "react-router";
import {Row, Col, Panel} from "react-bootstrap";
import TagList from "../TagList";

let RunDetailsPanel = React.createClass({
  render: function () {
    const workflowLink = 'workflows/view/' + this.props.run.workflow.id + '/' + this.props.run.workflow.version;
    return (
      <Panel header="Run details"
             className="accio-view-panel"
             collapsible={true}
             defaultExpanded={true}>
        <Row>
          <Col sm={2} className="accio-view-label">Workflow</Col>
          <Col sm={10}>
            <Link to={workflowLink}>{this.props.run.workflow.id}: {this.props.run.workflow.name}</Link>
          </Col>
        </Row>
        <Row>
          <Col sm={2} className="accio-view-label">Owner</Col>
          <Col sm={10}>{this.props.run.owner.name}</Col>
        </Row>
        <Row>
          <Col sm={2} className="accio-view-label">Tags</Col>
          <Col sm={10}><TagList tags={this.props.run.tags}/></Col>
        </Row>
      </Panel>
    );
  }
});

RunDetailsPanel.propTypes = {
  run: React.PropTypes.object.isRequired
};

export default RunDetailsPanel;
