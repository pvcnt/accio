import React from "react";
import xhr from "../../utils/xhr";
import RunList from "./RunList";
import {toPairs} from "lodash";

let RunListContainer = React.createClass({
  getInitialState: function () {
    return {
      data: {results: null, total_count: 0},
      query: {},
      page: 1,
    };
  },

  _loadData: function (state) {
    const qs = toPairs(state.query).map(pair => pair[0] + '=' + encodeURIComponent(pair[1])).join('&')
    xhr('/api/v1/run?per_page=25&page=' + state.page + '&' + qs)
      .then(data => this.setState({data: data}));
  },

  _handleChange: function (state) {
    this._loadData(state);
  },

  componentDidMount: function () {
    this._loadData(this.state);
  },

  render: function () {
    return <RunList page={this.state.page}
                    query={this.state.query}
                    runs={this.state.data.results}
                    totalCount={this.state.data.total_count}
                    onChange={this._handleChange}/>;
  }
});

export default RunListContainer;
