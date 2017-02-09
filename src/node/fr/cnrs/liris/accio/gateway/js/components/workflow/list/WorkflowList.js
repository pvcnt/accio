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
import {Grid, Row, Col, Pager} from 'react-bootstrap'
import Spinner from 'react-spinkit'
import WorkflowFilter from './WorkflowFilter'
import WorkflowTable from './WorkflowTable'

class WorkflowList extends React.Component {
  @autobind
  _hasNextPage() {
    const maxPages = Math.ceil(this.props.totalCount / 25)
    return this.props.page < maxPages
  }

  @autobind
  _hasPreviousPage() {
    return this.props.page > 1
  }

  @autobind
  _handleFilterChange(value) {
    this.props.onChange({query: value, page: this.props.page})
  }

  @autobind
  _handlePageChange(eventKey, e) {
    e.nativeEvent.preventDefault()
    if ('next' === eventKey && this._hasNextPage()) {
      this.props.onChange({query: this.props.query, page: this.props.page + 1})
    } else if ('previous' === eventKey && this._hasPreviousPage()) {
      this.props.onChange({query: this.props.query, page: this.props.page - 1})
    }
  }

  render() {
    return (
      <Grid className="accio-list">
        <Row>
          <Col sm={5}>
            <h2 className="accio-title">
              <img src="images/stack-32px.png"/>
              Workflows
            </h2>
          </Col>
          <Col sm={7}>
            <WorkflowFilter onSubmit={this._handleFilterChange}/>
          </Col>
        </Row>

        {(null !== this.props.workflows) ? <WorkflowTable workflows={this.props.workflows}/> : <Spinner spinnerName="three-bounce"/>}

        <Pager onSelect={this._handlePageChange}>
          <Pager.Item previous disabled={!this._hasPreviousPage()} href="#" eventKey="previous">
            &larr; Previous page
          </Pager.Item>
          <Pager.Item next disabled={!this._hasNextPage()} href="#" eventKey="next">
            Next page &rarr;
          </Pager.Item>
        </Pager>
      </Grid>
    )
  }
}

WorkflowList.propTypes = {
  onChange: React.PropTypes.func.isRequired,
  page: React.PropTypes.number.isRequired,
  query: React.PropTypes.object.isRequired,
  workflows: React.PropTypes.array,
  totalCount: React.PropTypes.number.isRequired,
}
WorkflowList.defaultProps = {
  onChange: noop,
}

export default WorkflowList