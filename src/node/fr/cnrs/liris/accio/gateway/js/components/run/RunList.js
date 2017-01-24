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
import {LinkContainer} from "react-router-bootstrap";
import RunTable from "./RunTable";
import RunFilter from "./RunFilter";
import {Grid, Row, Col, Nav, NavItem, Button, Pager, Glyphicon} from "react-bootstrap";
import Spinner from "react-spinkit";

let RunList = React.createClass({
  getInitialState: function() {
    return {
      section: 'mine',
    };
  },

  getDefaultProps: function () {
    return {
      onChange: noop,
    };
  },

  _hasNextPage: function () {
    const maxPages = Math.ceil(this.props.totalCount / 25);
    return this.props.page < maxPages;
  },

  _hasPreviousPage: function () {
    return this.props.page > 1;
  },

  _handleFilterChange: function (value) {
    this.setState({section: 'custom'});
    this.props.onChange({query: value, page: this.props.page});
  },

  _handlePageChange: function (eventKey, e) {
    e.nativeEvent.preventDefault();
    if ('next' === eventKey && this._hasNextPage()) {
      this.props.onChange({query: this.props.query, page: this.props.page + 1});
    } else if ('previous' === eventKey && this._hasPreviousPage()) {
      this.props.onChange({query: this.props.query, page: this.props.page - 1});
    }
  },

  render: function () {
    return (
      <Grid className="accio-list">
        <Row>
          <Col sm={5}>
            <h2 className="accio-title">
              <img src="images/bars-32px.png"/>
              Runs
            </h2>
          </Col>
          <Col sm={7}>
            <RunFilter onSubmit={this._handleFilterChange}/>
          </Col>
        </Row>

        <div className="accio-actions">
          <Button><Glyphicon glyph="th" /> Compare</Button>
        </div>

        <p>
          Runs are instantiations of workflows, for some parameters.
          They produce artifacts and various metrics, that can be viewed and compared here.
        </p>

        {(null !== this.props.runs) ? <RunTable runs={this.props.runs}/> : <Spinner spinnerName="three-bounce"/>}

        <Pager onSelect={this._handlePageChange}>
          <Pager.Item previous disabled={!this._hasPreviousPage()} href="#" eventKey="previous">
            &larr; Previous page
          </Pager.Item>
          <Pager.Item next disabled={!this._hasNextPage()} href="#" eventKey="next">
            Next page &rarr;
          </Pager.Item>
        </Pager>
      </Grid>
    );
  }
});

RunList.propTypes = {
  onChange: React.PropTypes.func.isRequired,
  page: React.PropTypes.number.isRequired,
  query: React.PropTypes.object.isRequired,
  runs: React.PropTypes.array,
  totalCount: React.PropTypes.number.isRequired
};

export default RunList;
