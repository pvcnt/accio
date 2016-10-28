import React from "React";
import {filter, find, isEmpty} from "lodash";
import {LinkContainer} from "react-router-bootstrap";
import CopyToClipboard from "react-copy-to-clipboard";
import {Grid, Button, Glyphicon} from "react-bootstrap";
import RunStatusPanel from "./RunStatusPanel";
import RunDetailsPanel from "./RunDetailsPanel";
import SummaryPanelContainer from "./SummaryPanelContainer";
import GraphView from "../GraphView";
import LazyPanel from "../LazyPanel";

let RunView = React.createClass({
  getInitialState: function () {
    return {
      graphHeight: 500,
    };
  },

  _handleLargerGraphClick: function () {
    this.setState({graphHeight: this.state.graphHeight + 50});
  },

  _handleSmallerGraphClick: function () {
    this.setState({graphHeight: this.state.graphHeight - 50});
  },

  render: function () {
    const {run} = this.props;

    const summaryPanels =
      filter(run.per_node, nodeRun => {
        const node = find(run.graph.nodes, node => node.name === nodeRun.node);
        const opDef = find(run.graph.operators, opDef => opDef.name === node.op);
        return !isEmpty(opDef.summaries);
      }).map((nodeRun, idx) => {
        return (
          <LazyPanel key={idx}
                     header={'Summaries of ' + nodeRun.node}
                     className="accio-view-panel"
                     collapsible={true}
                     defaultExpanded={false}>
            <SummaryPanelContainer run={run.id} node={nodeRun.node}/>
          </LazyPanel>
        );
      });

    return (
      <Grid>
        <h2 className="accio-title">
          <img src="images/bars-32px.png"/> {run.name}
        </h2>

        <div className="accio-actions">
          <LinkContainer to={{path: '/runs/create', query: {copy: run.id}}}>
            <Button><Glyphicon glyph="duplicate"/> Clone run</Button>
          </LinkContainer>

          <Button href={'/api/run/' + run.id + '?download=true'}>
            <Glyphicon glyph="save"/> Download run as JSON
          </Button>

          <CopyToClipboard text={run.id}>
            <Button><Glyphicon glyph="copy"/> Copy ID to clipboard</Button>
          </CopyToClipboard>
        </div>

        <RunStatusPanel run={run}/>

        <RunDetailsPanel run={run} experiment={this.props.experiment}/>

        <LazyPanel header={<h3>Run graph</h3>}
                   className="accio-view-panel accio-view-panel-graph"
                   collapsible={true}
                   defaultExpanded={false}>
          <GraphView graph={run.graph} run={run} height={this.state.graphHeight}/>
        </LazyPanel>

        {summaryPanels}
      </Grid>
    );
  }
});

RunView.propTypes = {
  run: React.PropTypes.object.isRequired,
  experiment: React.PropTypes.object.isRequired
};

export default RunView;
