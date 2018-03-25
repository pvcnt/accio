/*
 * Accio is a program whose purpose is to study location privacy.
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
import {sortBy, noop} from 'lodash'
import CopyToClipboard from 'react-copy-to-clipboard'
import {Grid, Button, Glyphicon, Checkbox, Form} from 'react-bootstrap'
import autobind from 'autobind-decorator'
import xhr from '../../../utils/xhr'
import StatusPanel from './StatusPanel'
import DetailsPanel from './DetailsPanel'
import MetadataPanel from './MetadataPanel'
import ParamsPanel from './ParamsPanel'
import LazyPanel from '../../LazyPanel'
import ConfirmModal from '../../ConfirmModal'
import RunArtifactsContainer from './RunArtifactsContainer'

class RunView extends React.Component {
  constructor(props) {
    super(props)
    this.state = {killShown: false, deleteShown: false}
  }

  @autobind
  _handleKillShow() {
    this.setState({killShown: true})
  }

  @autobind
  _handleKillCancel() {
    this.setState({killShown: false})
  }

  @autobind
  _handleKillConfirm() {
    xhr('/api/v1/run/' + this.props.run.id + '/kill', {method: 'POST'}, false)
      .then(data => {
        this.setState({killShown: false})
        this.props.onChange(data)
      })
  }

  render() {
    const {run} = this.props;
    const nodes = sortBy(run.state.nodes, ['started_at'])
    const artifactPanels = (!run.children.length && run.state.completed_at)
      ? nodes.map((node, idx) => {
        if (node.status == 'success') {
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
      }) : null

    return (
      <Grid>
        <h2 className="accio-title">
          <img src="images/bars-32px.png"/>
          {run.parent ? (run.parent.name ? run.parent.name : 'Untitled run #' + run.parent.id) + ' / ' : ''}
          {run.name ? run.name : 'Untitled run #' + run.id}
        </h2>

        {this.state.killShown
          ? <ConfirmModal
              title="Cancel run"
              question="Are you sure that you want to cancel this run?"
              onCancel={this._handleKillCancel}
              onConfirm={this._handleKillConfirm}/>
          : null}

        <div className="accio-actions">
          {(run.state.status === 'running' || run.state.status === 'scheduled')
            ? <Button onClick={this._handleKillShow} bsStyle="primary">
                <Glyphicon glyph="exclamation-sign"/> Cancel run
            </Button>
            : null}
          <Button href={'/api/v1/run/' + run.id + '?download=true'}>
            <Glyphicon glyph="save"/> Download as JSON
          </Button>
          <CopyToClipboard text={run.id}>
            <Button><Glyphicon glyph="copy"/> Copy ID to clipboard</Button>
          </CopyToClipboard>
        </div>

        <StatusPanel run={run}/>
        <MetadataPanel run={run} onChange={this.props.onChange}/>
        <DetailsPanel run={run}/>
        {!run.children.length ? <ParamsPanel run={run}/> : null}

        {artifactPanels}
      </Grid>
    )
  }
}

RunView.propTypes = {
  run: React.PropTypes.object.isRequired,
  onChange: React.PropTypes.func.isRequired,
}
RunView.defaultProps = {
  onChange: noop,
}

export default RunView
