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
import {noop} from "lodash";
import {Row, Col, Button, FormControl, InputGroup, Glyphicon} from "react-bootstrap";

let WorkflowFilter = React.createClass({
  getInitialState: function () {
    return {
      value: this.props.defaultValue,
    };
  },

  getDefaultProps: function () {
    return {
      defaultValue: '',
      onSubmit: noop
    };
  },

  _handleSubmit: function (e) {
    e.nativeEvent.preventDefault();
    this.props.onSubmit({name: this.state.value})
  },

  _handleChange: function (e) {
    this.setState({value: e.target.value});
  },

  render: function () {
    return (
      <form onSubmit={this._handleSubmit} className="accio-list-filter">
        <InputGroup>
          <FormControl type="text"
                      value={this.state.value}
                      onChange={this._handleChange}
                      placeholder="Search by name or owner..."/>
          <InputGroup.Button>
            <Button type="submit"><Glyphicon glyph="search" /></Button>
          </InputGroup.Button>
        </InputGroup>
      </form>
    );
  }
});

WorkflowFilter.propTypes = {
  defaultValue: React.PropTypes.string.isRequired,
  onSubmit: React.PropTypes.func.isRequired,
};

export default WorkflowFilter;
