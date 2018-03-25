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
import nl2br from 'react-nl2br'
import autobind from 'autobind-decorator'
import {Row, Col, Panel, Button, Glyphicon} from 'react-bootstrap'
import {noop} from 'lodash'
import EditMetadataModal from './EditMetadataModal'
import TagList from '../../TagList'

class MetadataPanel extends React.Component {
  constructor(props) {
    super(props)
    this.state = {shown: false}
  }

  @autobind
  _handleShow(e) {
    e.nativeEvent.preventDefault()
    this.setState({shown: true})
  }

  @autobind
  _handleClose() {
    this.setState({shown: false})
  }

  @autobind
  _handleSubmit(newRun) {
    this.setState({shown: false})
    this.props.onChange(newRun)
  }

  render() {
    const {run} = this.props
    return (
      <div>
        <Button onClick={this._handleShow} style={{float: 'right', marginRight: '4px', marginTop: '4px'}} bsSize="small">
          <Glyphicon glyph="pencil"/>&nbsp;Edit
        </Button>
        <Panel header={run.parent ? 'Parent run metadata' : 'Run metadata'}
               className="accio-view-panel"
               collapsible={true}
               defaultExpanded={false}>
          <Row>
            <Col sm={2} className="accio-view-label">Name</Col>
            <Col sm={10}>{run.parent ? run.parent.name : run.name}</Col>
          </Row>
          <Row>
            <Col sm={2} className="accio-view-label">Notes</Col>
            <Col sm={10}>{nl2br(run.parent ? run.parent.notes : run.notes)}</Col>
          </Row>
          <Row>
            <Col sm={2} className="accio-view-label">Tags</Col>
            <Col sm={10}><TagList tags={run.parent ? run.parent.tags : run.tags}/></Col>
          </Row>
        </Panel>
        {this.state.shown
          ? <EditMetadataModal
            run={run.parent ? run.parent : run}
            onSubmit={this._handleSubmit}
            onClose={this._handleClose} />
          : null}
      </div>
    )
  }
}

MetadataPanel.propTypes = {
  run: React.PropTypes.object.isRequired,
  onChange: React.PropTypes.func.isRequired,
}
MetadataPanel.defaultProps = {
  onChange: noop,
}

export default MetadataPanel
