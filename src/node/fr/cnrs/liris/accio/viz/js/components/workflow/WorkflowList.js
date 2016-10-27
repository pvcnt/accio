import React from "React";
import {noop} from "lodash";
import moment from "moment";
import {Link} from "react-router";
import {Grid, Row, Col, Button, Table, FormControl, Pager} from "react-bootstrap";
import Spinner from "react-spinkit";
import WorkflowFilter from "./WorkflowFilter";
import WorkflowTable from "./WorkflowTable";

let WorkflowList = React.createClass({
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
              <img src="images/stack-32px.png"/>
              Workflows
            </h2>
          </Col>
          <Col sm={7}>
            <WorkflowFilter onSubmit={this._handleFilterChange}/>
          </Col>
        </Row>

        <p>
          Workflows define a graph of operations to execute.
          Their are a way to package a given experiment and make it reproducible.
          You can find here all created workflows, search for them and view their details.
        </p>

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
    );
  }
});

WorkflowList.propTypes = {
  onChange: React.PropTypes.func.isRequired,
  page: React.PropTypes.number.isRequired,
  query: React.PropTypes.string.isRequired,
  workflows: React.PropTypes.array,
  totalCount: React.PropTypes.number.isRequired
};

export default WorkflowList;
