import React from "React";
import {find, some, map} from "lodash";
import {Row, Col, Panel} from "react-bootstrap";

let WorkflowParamsPanel = React.createClass({
  render: function () {
    const rows = map(this.props.workflow.params, (deps, name) => {
      const depsAndParamDefs = deps.map(dep => {
        const pos = dep.indexOf("/");
        const nodeName = dep.substring(0, pos);
        const port = dep.substring(pos + 1);
        const node = find(this.props.workflow.graph.nodes, node => node.name === nodeName);
        const opDef = find(this.props.workflow.graph.operators, opDef => opDef.name === node.op);
        return [dep, find(opDef.params, paramDef => paramDef.name === port)];
      });
      const rows = depsAndParamDefs.map((pair, idx) => {
        return <div key={idx}>
          <b>{pair[0]}</b>{(pair[1].help) ? ': ' + pair[1].help : ''}
        </div>;
      });
      const isRequired = some(depsAndParamDefs, pair => !pair[1].is_optional);
      return <Row key={name}>
        <Col md={2} className="accio-view-label">{name}</Col>
        <Col md={9}>{rows}</Col>
        <Col md={1}>{isRequired ? <span className="accio-run-param-required">Required</span> : null}</Col>
      </Row>;
    });
    return (
      <Panel header="Workflow parameters"
             className="accio-view-panel"
             collapsible={true}
             defaultExpanded={true}>
        {rows.length > 0 ? rows : <em>No workflow parameter.</em>}
      </Panel>
    );
  }
});

WorkflowParamsPanel.propTypes = {
  workflow: React.PropTypes.object.isRequired
};

export default WorkflowParamsPanel;
