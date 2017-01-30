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
import {Modal} from 'react-bootstrap'
import {noop} from 'lodash'

class ErrorModal extends React.Component {
  render() {
    const {nodeName, error} = this.props
    return (
      <Modal show={true} onHide={this._handleErrorModalClose} keyboard={true} bsSize="large">
        <Modal.Header closeButton>
          <Modal.Title>Exception for {nodeName}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {error.root.classifier}: {error.root.message}
          <pre>{error.root.stacktrace.join("\n")}</pre>
        </Modal.Body>
      </Modal>
    )
  }
}

ErrorModal.propTypes = {
  nodeName: React.PropTypes.string.isRequired,
  error: React.PropTypes.object.isRequired,
  onClose: React.PropTypes.func.isRequired,
}
ErrorModal.defaultProps = {
  onClose: noop,
}

export default ErrorModal