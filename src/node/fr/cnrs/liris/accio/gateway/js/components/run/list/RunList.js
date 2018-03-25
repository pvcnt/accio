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
import autobind from 'autobind-decorator'
import {Grid, Row, Col, Button, Pager, Glyphicon} from 'react-bootstrap';
import Spinner from 'react-spinkit'
import RunTable from './RunTable'
import RunFilter from './RunFilter'
import {RUNS_PER_PAGE} from '../../../constants'

class RunList extends React.Component {
  @autobind
  _hasNextPage() {
    const maxPages = Math.ceil(this.props.totalCount / RUNS_PER_PAGE)
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
            <h2 className="accio-title"><img src="images/bars-32px.png"/> Runs</h2>
          </Col>
          <Col sm={7}>
            <RunFilter onSubmit={this._handleFilterChange}/>
          </Col>
        </Row>

        <Row className="accio-actions">
          <Col sm={12}>
            <Button><Glyphicon glyph="th" /> Compare</Button>
            <Button disabled bsStyle="primary"><Glyphicon glyph="plus" /> Create run</Button>
          </Col>
        </Row>

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
    )
  }
}

RunList.propTypes = {
  onChange: React.PropTypes.func.isRequired,
  page: React.PropTypes.number.isRequired,
  query: React.PropTypes.object.isRequired,
  runs: React.PropTypes.array,
  totalCount: React.PropTypes.number.isRequired,
}
RunList.defaultProps = {
  onChange: noop,
}

export default RunList
