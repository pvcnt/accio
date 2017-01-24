import React from "react";
import moment from "moment";
import {Row, Col, Badge, Glyphicon, Label, Modal, Panel} from "react-bootstrap";

let RunStatusPanel = React.createClass({
  getInitialState: function () {
    return {
      error: null,
    };
  },

  _handleErrorModalClose: function () {
    this.setState({error: null});
  },

  _handleErrorModalShow: function (e, node, data) {
    e.nativeEvent.preventDefault();
    this.setState({error: {data: data, node: node}});
  },

  _getState: function (state) {
    if (state.completed_at) {
      const millis = state.completed_at - state.started_at;
      const successful = (state.status == 'success');
      return {
        duration: moment.duration(millis).humanize(),
        text: (successful) ? 'successful' : 'errored',
        glyph: (successful) ? 'ok' : 'remove',
        successful: successful,
      };
    } else if (state.started_at) {
      const millis = moment().valueOf() - state.started_at;
      return {
        duration: moment.duration(millis).humanize(),
        text: 'running',
        glyph: 'refresh',
      };
    } else {
      return {
        duration: '–',
        text: 'queued',
        glyph: 'inbox',
      };
    }
  },

  render: function () {
    const run = this.props.run;
    const runState = this._getState(run.state);

    const nodeTreeRows = run.state.nodes.map((node, idx) => {
      const nodeState = this._getState(node);
      const label = (node.completed_at)
        ? <Label bsStyle={(nodeState.successful) ? 'success' : 'danger'}>Ran for {nodeState.duration}</Label>
        : <Label bsStyle="info">Running for {nodeState.duration}</Label>;
      const showError = (node.result && node.result.error)
        ? (
        <span>
        <Glyphicon glyph="warning-sign"/>
        <a href="#" onClick={e => this._handleErrorModalShow(e, node.node_name, node.error)}>Show exception details</a>
      </span>
      ) : null;

      return (
        <Row key={idx}>
          <Col sm={3}><Glyphicon glyph={nodeState.glyph}/> {node.node_name}</Col>
          <Col sm={9}>{label} {showError}</Col>
        </Row>
      );
    });

    const errorModal = (null !== this.state.error) ? (
      <Modal show={true} onHide={this._handleErrorModalClose} keyboard={true} bsSize="large">
        <Modal.Header closeButton>
          <Modal.Title>Exception for {this.state.error.node}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {this.state.error.data.class_name}: {this.state.error.data.message}
          <pre>{this.state.error.data.stack_trace.join("\n")}</pre>
          pre>
        </Modal.Body>
      </Modal>
    ) : null;

    const header = <h3><Glyphicon glyph={runState.glyph}/> Run execution: {runState.text}</h3>;

    return (
      <Panel header={header}
             className="accio-view-panel"
             collapsible={true}
             defaultExpanded={true}>
        <Row>
          <Col sm={2} className="accio-view-label">Started</Col>
          <Col sm={10}>{moment(run.started_at).format('MMM Do YYYY, hh:mma')}</Col>
        </Row>
        {run.completed ? <Row>
          <Col sm={2} className="accio-view-label">Completed</Col>
          <Col sm={10}>{moment(run.completed_at).format('MMM Do YYYY, hh:mma')}</Col>
        </Row> : null}
        <Row>
          <Col sm={2} className="accio-view-label">Duration</Col>
          <Col sm={10}>{runState.duration}</Col>
        </Row>
        <Row>
          <Col sm={2} className="accio-view-label">Nodes execution</Col>
          <Col sm={10} className="accio-run-nodes-tree">
            {nodeTreeRows}
          </Col>
        </Row>
        {errorModal}
      </Panel>
    );
  }
});

RunStatusPanel.propTypes = {
  run: React.PropTypes.object.isRequired
};

export default RunStatusPanel;
