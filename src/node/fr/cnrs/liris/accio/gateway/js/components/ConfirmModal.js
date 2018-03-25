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
import {noop} from 'lodash'
import {Modal, Button} from 'react-bootstrap'

class ConfirmModal extends React.Component {
  render() {
    return (
      <Modal show={true} onHide={this.props.onCancel} keyboard={true} bsSize="large">
        <Modal.Header closeButton>
          <Modal.Title>{this.props.title}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <p>{this.props.question}</p>
          <div className="center">
            <Button bsStyle="primary" onClick={this.props.onConfirm}>Yes</Button>
            <Button style={{marginLeft: 5}} onClick={this.props.onCancel}>No</Button>
          </div>
        </Modal.Body>
      </Modal>
    )
  }
}

ConfirmModal.propTypes = {
  title: React.PropTypes.string.isRequired,
  question: React.PropTypes.string.isRequired,
  onConfirm: React.PropTypes.func.isRequired,
  onCancel: React.PropTypes.func.isRequired,
}
ConfirmModal.defaultProps = {
  onConfirm: noop,
  onCancel: noop,
}

export default ConfirmModal
