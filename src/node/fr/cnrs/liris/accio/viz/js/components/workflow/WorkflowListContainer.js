import React from "react";
import xhr from "../../utils/xhr";
import WorkflowList from "./WorkflowList";

let WorkflowListContainer = React.createClass({
  getInitialState: function () {
    return {
      data: {results: null, total_count: 0},
      query: '',
      page: 1,
    };
  },

  _loadData: function (state) {
    xhr('/api/workflow?q=' + encodeURIComponent(state.query) + '&page=' + state.page)
      .then(data => this.setState(Object.assign({}, state, {data: data})));
  },

  _handleChange: function (state) {
    this._loadData(state);
  },

  componentDidMount: function () {
    this._loadData(this.state);
  },

  render: function () {
    return <WorkflowList page={this.state.page}
                         query={this.state.query}
                         workflows={this.state.data.results}
                         totalCount={this.state.data.total_count}
                         onChange={this._handleChange}/>;
  }
});

export default WorkflowListContainer;
