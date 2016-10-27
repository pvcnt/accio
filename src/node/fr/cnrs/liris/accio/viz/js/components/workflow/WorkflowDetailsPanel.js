import React from "React";
import moment from "moment";
import {Row, Col, Panel} from "react-bootstrap";
import TagList from "../TagList";

let WorkflowDetailsPanel = React.createClass({
  render: function () {
    const workflow = this.props.workflow;
    return (
      <Panel header="Workflow details"
             className="accio-view-panel"
             collapsible={true}
             defaultExpanded={true}>
        <Row>
          <Col md={2} className="accio-view-label">ID</Col>
          <Col md={10}>{workflow.id}</Col>
        </Row>
        <Row>
          <Col md={2} className="accio-view-label">Created</Col>
          <Col md={10}>{moment(workflow.created).format('MMM Do YYYY, hh:mma')}</Col>
        </Row>
        <Row>
          <Col md={2} className="accio-view-label">Owner</Col>
          <Col md={10}>{workflow.owner.name}</Col>
        </Row>
        <Row>
          <Col md={2} className="accio-view-label">Notes</Col>
          <Col md={10}>{(workflow.notes) ? workflow.notes : '–'}</Col>
        </Row>
        <Row>
          <Col sm={2} className="accio-view-label">Tags</Col>
          <Col sm={10}><TagList tags={workflow.tags}/></Col>
        </Row>
      </Panel>
    );
  }
});

WorkflowDetailsPanel.propTypes = {
  workflow: React.PropTypes.object.isRequired
};

export default WorkflowDetailsPanel;
