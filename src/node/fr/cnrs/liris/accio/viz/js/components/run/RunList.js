import React from "React";
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

  _handleSectionChange: function(eventKey) {
    this.setState({section: eventKey});
    const query = ('mine' === eventKey) ? 'owner:vincent' : ('custom' === eventKey) ? this.props.query : '';
    this.props.onChange({query: query, page: this.props.page});
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
          <LinkContainer to="/runs/create">
            <Button><Glyphicon glyph="plus" /> Launch new run</Button>
          </LinkContainer>
        </div>

        <p>
          Runs are instantiations of workflows, for some parameters.
          They produce summaries and various statistics, that can be viewed and compared here.
        </p>

        <div className="accio-list-nav">
          <Nav bsStyle="pills" activeKey={this.state.section} onSelect={this._handleSectionChange}>
            <NavItem eventKey="mine">My runs</NavItem>
            <NavItem eventKey="all">All runs</NavItem>
            <NavItem eventKey="custom" disabled={!this.state.query}>Custom</NavItem>
          </Nav>
        </div>

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
  query: React.PropTypes.string.isRequired,
  runs: React.PropTypes.array,
  totalCount: React.PropTypes.number.isRequired
};

export default RunList;
