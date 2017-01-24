import React from "react";
import {filter, find, isEmpty} from "lodash";
import {LinkContainer} from "react-router-bootstrap";
import CopyToClipboard from "react-copy-to-clipboard";
import {Grid, Button, Glyphicon} from "react-bootstrap";
import RunStatusPanel from "./RunStatusPanel";
import RunDetailsPanel from "./RunDetailsPanel";
import SummaryPanelContainer from "./SummaryPanelContainer";
import LazyPanel from "../LazyPanel";

let RunView = React.createClass({
  getInitialState: function () {
    return {};
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
          <img src="images/bars-32px.png"/> {run.name ? run.name : 'Untitled run #' + run.id.value}
        </h2>

        <div className="accio-actions">
          <Button href={'/api/v1/run/' + run.id.value + '?download=true'}>
            <Glyphicon glyph="save"/> Download as JSON
          </Button>

          <CopyToClipboard text={run.id.value}>
            <Button><Glyphicon glyph="copy"/> Copy ID to clipboard</Button>
          </CopyToClipboard>
        </div>

        <RunStatusPanel run={run}/>

        <RunDetailsPanel run={run}/>
      </Grid>
    );
  }
});

RunView.propTypes = {
  run: React.PropTypes.object.isRequired,
};

export default RunView;
