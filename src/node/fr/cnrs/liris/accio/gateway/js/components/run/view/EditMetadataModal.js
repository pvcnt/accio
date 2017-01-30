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

import React from 'react'
import {noop} from 'lodash'
import autobind from 'autobind-decorator'
import {FormGroup, ControlLabel, FormControl, Modal, Button} from 'react-bootstrap'
import xhr from '../../../utils/xhr'

class EditMetadataModal extends React.Component {
  constructor(props) {
    super(props)
    this.state = {name: this.props.run.name, notes: this.props.run.notes, tags: this.props.run.tags}
  }

  @autobind
  _handleSubmit(e) {
    e.nativeEvent.preventDefault()
    const newRun = Object.assign({}, this.props.run, this.state)
    xhr('/api/v1/run/' + newRun.id, {method: 'POST', body: JSON.stringify(this.state)}, false)
      .then(data => this.props.onSubmit(newRun))
  }

  @autobind
  _handleNameChange(e) {
    this.setState({name: e.target.value})
  }

  @autobind
  _handleNotesChange(e) {
    this.setState({notes: e.target.value})
  }

  @autobind
  _handleTagsChange(e) {
    this.setState({tags: e.target.value.split(',')})
  }

  @autobind
  _handleClose() {
    this.props.onClose()
  }

  render() {
    return (
      <Modal show={true} onHide={this._handleClose} keyboard={true} bsSize="large">
        <Modal.Header closeButton>
          <Modal.Title>Edit run metadata</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <form onSubmit={this._handleSubmit}>
            <FormGroup>
               <ControlLabel>Name</ControlLabel>
               <FormControl
                 type="text"
                 value={this.state.name}
                 onChange={this._handleNameChange}/>
            </FormGroup>
            <FormGroup>
              <ControlLabel>Notes</ControlLabel>
              <FormControl
                  componentClass="textarea"
                  style={{height: '80px'}}
                  value={this.state.notes}
                  onChange={this._handleNotesChange}/>
            </FormGroup>
            <FormGroup>
              <ControlLabel>Tags</ControlLabel>
              <FormControl
                  type="text"
                  value={this.state.tags.join(',')}
                  onChange={this._handleTagsChange}/>
            </FormGroup>
            <Button type="submit" bsStyle="primary">Submit</Button>
          </form>
        </Modal.Body>
      </Modal>
    )
  }
}

EditMetadataModal.propTypes = {
  run: React.PropTypes.object.isRequired,
  onSubmit: React.PropTypes.func.isRequired,
  onClose: React.PropTypes.func.isRequired,
}
EditMetadataModal.defaultProps = {
  onSubmit: noop,
  onClose: noop,
}

export default EditMetadataModal
