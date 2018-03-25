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

import React from "react";
import {noop} from "lodash";
import {LinkContainer} from "react-router-bootstrap";
import RunTable from "./RunTable";
import RunFilter from "./RunFilter";
import {Grid, Row, Col, Nav, NavItem, Button, Pager, Glyphicon} from "react-bootstrap";
import Spinner from "react-spinkit";

class PaginatedRunTable extends React.Component {
  _hasNextPage() {
    const maxPages = Math.ceil(this.props.totalCount / 25);
    return this.props.page < maxPages;
  }

  _hasPreviousPage() {
    return this.props.page > 1;
  }

  _handlePageChange(eventKey, e) {
    e.nativeEvent.preventDefault();
    if ('next' === eventKey && this._hasNextPage()) {
      this.props.onChange({query: this.props.query, page: this.props.page + 1});
    } else if ('previous' === eventKey && this._hasPreviousPage()) {
      this.props.onChange({query: this.props.query, page: this.props.page - 1});
    }
  }

  render() {
    return (
      <div>
        {(null !== this.props.runs) ? <RunTable runs={this.props.runs}/> : <Spinner spinnerName="three-bounce"/>}
        <Pager onSelect={this._handlePageChange}>
          <Pager.Item previous disabled={!this._hasPreviousPage()} href="#" eventKey="previous">
            &larr; Previous page
          </Pager.Item>
          <Pager.Item next disabled={!this._hasNextPage()} href="#" eventKey="next">
            Next page &rarr;
          </Pager.Item>
        </Pager>
      </div>
    )
  }
}

PaginatedRunTable.propTypes = {
  onChange: React.PropTypes.func.isRequired,
  page: React.PropTypes.number.isRequired,
  query: React.PropTypes.object.isRequired,
  runs: React.PropTypes.array,
  totalCount: React.PropTypes.number.isRequired
}
PaginatedRunTable.defaultProps = {
  onChange: noop
}

export default PaginatedRunTable
