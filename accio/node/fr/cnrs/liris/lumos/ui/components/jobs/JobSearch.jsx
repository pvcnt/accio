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

import React from 'react';
import PropTypes from 'prop-types';
import { InputGroup, Button, Intent } from '@blueprintjs/core';

class JobSearch extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      value: props.value,
    };
  }

  handleChange(e) {
    this.setState({ value: e.target.value });
  }

  handleBlur(e) {
    if (e.target.value !== this.props.value) {
      this.props.onChange(e.target.value);
    }
  }

  handleSubmit() {
    if (this.state.value !== this.props.value) {
      this.props.onChange(this.state.value);
    }
  }

  render() {
    return (
      <div className="job-search">
        <form onSubmit={() => this.handleSubmit()}>
          <InputGroup leftIcon="search"
                      value={this.state.value}
                      placeholder="Search by labels…"
                      fill={true}
                      onChange={e => this.handleChange(e)}
                      onBlur={e => this.handleBlur(e)}/>
        </form>
      </div>
    );
  }
}

JobSearch.propTypes = {
  value: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
};

export default JobSearch;