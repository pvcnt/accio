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
import {sortBy} from "lodash";
import {LinkContainer} from "react-router-bootstrap";
import CopyToClipboard from "react-copy-to-clipboard";
import {Grid, Button, Glyphicon} from "react-bootstrap";
import RunStatusPanel from "./RunStatusPanel";
import RunDetailsPanel from "./RunDetailsPanel";
import RunMetadataPanel from "./RunMetadataPanel";
import RunParamsPanel from "./RunParamsPanel";
import LazyPanel from "../../LazyPanel";
import RunArtifactsContainer from './RunArtifactsContainer'

class RunView extends React.Component {
  render() {
    const {run} = this.props;
    const nodes = sortBy(run.state.nodes, ['started_at'])
    const artifactPanels = (!run.children && run.state.completed_at)
      ? nodes.map((node, idx) => {
        if (node.status == 'success') {
          return <LazyPanel
            key={idx}
            header={'Outputs: ' + node.node_name}
            className="accio-view-panel"
            collapsible={true}
            defaultExpanded={false}>
            <RunArtifactsContainer runId={run.id} nodeName={node.node_name}/>
          </LazyPanel>
        } else {
          return null
        }
      }) : null

    return (
      <Grid>
        <h2 className="accio-title">
          <img src="images/bars-32px.png"/> {run.name ? run.name : 'Untitled run #' + run.id}
        </h2>

        <div className="accio-actions">
          <Button href={'/api/v1/run/' + run.id + '?download=true'}>
            <Glyphicon glyph="save"/> Download as JSON
          </Button>

          <CopyToClipboard text={run.id}>
            <Button><Glyphicon glyph="copy"/> Copy ID to clipboard</Button>
          </CopyToClipboard>
        </div>

        <RunStatusPanel run={run}/>
        <RunMetadataPanel run={run}/>
        <RunDetailsPanel run={run}/>
        {!run.children ? <RunParamsPanel run={run}/> : null}

        {artifactPanels}
      </Grid>
    );
  }
}

RunView.propTypes = {
  run: React.PropTypes.object.isRequired,
};

export default RunView;
