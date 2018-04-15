/*
 * Accio is a platform to launch computer science experiments.
 * Copyright (C) 2016-2018 Vincent Primault <v.primault@ucl.ac.uk>
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

import React from 'react'
import {noop, sortBy} from 'lodash'
import CopyToClipboard from 'react-copy-to-clipboard'
import {Button, Glyphicon, Grid} from 'react-bootstrap'
import StatusPanel from './StatusPanel'
import DetailsPanel from './DetailsPanel'
import MetadataPanel from './MetadataPanel'
import ParamsPanel from './ParamsPanel'
import LazyPanel from '../../LazyPanel'
import RunArtifactsContainer from './RunArtifactsContainer'

class RunView extends React.Component {
  render() {
    const {run} = this.props;
    const nodes = sortBy(run.state.nodes, ['started_at'])
    const artifactPanels = (!run.children.length && run.state.completed_at)
      ? nodes.map((node, idx) => {
        if (node.status === 'success') {
          return <LazyPanel
            key={idx}
            header={'Outputs: ' + node.name}
            className="accio-view-panel"
            collapsible={true}
            defaultExpanded={false}>
            <RunArtifactsContainer runId={run.id} nodeName={node.name}/>
          </LazyPanel>
        } else {
          return null
        }
      }) : null;

    return (
      <Grid>
        <h2 className="accio-title">
          <img src="images/bars-32px.png"/>
          {run.title ? run.title : `Untitled run ${run.name}`}
        </h2>

        <div className="accio-actions">
          <Button href={'/api/v1/run/' + run.id + '?download=true'}>
            <Glyphicon glyph="save"/> Download as JSON
          </Button>
          <CopyToClipboard text={run.id}>
            <Button><Glyphicon glyph="copy"/> Copy ID to clipboard</Button>
          </CopyToClipboard>
        </div>

        <StatusPanel job={run}/>
        <MetadataPanel job={run}/>
        <DetailsPanel run={run}/>
        {!run.status.children ? <ParamsPanel run={run}/> : null}
        {artifactPanels}
      </Grid>
    )
  }
}

RunView.propTypes = {
  run: React.PropTypes.object.isRequired,
};

export default RunView;
