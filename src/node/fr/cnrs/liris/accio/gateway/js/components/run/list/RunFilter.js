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
import {Button, FormControl, InputGroup, Glyphicon} from 'react-bootstrap'
import autobind from 'autobind-decorator'

class RunFilter extends React.Component {
  constructor(props) {
    super(props)
    this.state = {value: this.props.defaultValue}
  }

  @autobind
  _handleClear() {
    this.setState({value: ''}, () => this.props.onSubmit({q: this.state.value}))
  }

  @autobind
  _handleSubmit(e) {
    e.nativeEvent.preventDefault()
    this.props.onSubmit({q: this.state.value})
  }

  @autobind
  _handleChange(e) {
    this.setState({value: e.target.value})
  }

  render() {
    return (
      <form onSubmit={this._handleSubmit} className="accio-list-filter">
        <InputGroup>
          <FormControl type="text"
                      value={this.state.value}
                      onChange={this._handleChange}
                      placeholder="Search by name, workflow, owner or tags..."/>
          <InputGroup.Button>
            {this.state.value ? <Button onClick={this._handleClear}><Glyphicon glyph="remove" /></Button> : null}
            <Button type="submit"><Glyphicon glyph="search" /></Button>
          </InputGroup.Button>
        </InputGroup>
      </form>
    );
  }
}

RunFilter.propTypes = {
  defaultValue: React.PropTypes.string.isRequired,
  onSubmit: React.PropTypes.func.isRequired,
}
RunFilter.defaultProps = {
  defaultValue: '',
  onSubmit: noop,
}

export default RunFilter;
