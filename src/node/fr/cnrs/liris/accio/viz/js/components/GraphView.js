import React from "React";
import d3plus from "d3plus";
import {uniqueId} from "lodash";
import {map, flatMap, fromPairs, toPairs, keys, identity, filter, head, uniqid} from "lodash";

let GraphView = React.createClass({
  getDefaultProps: function () {
    return {
      id: uniqueId(),
    };
  },

  _updateGraph: function () {
    const nodes = this.props.graph.map(node => {
      const params = fromPairs(filter(toPairs(node.inputs, (kv) => kv[1].value)).map(kv => ['in:' + kv[0], kv[1].value]));
      let color = '#5bc0de'; // Bootstrap's "info" style
      if (this.props.report) {
        const stats = head(filter(this.props.report.node_stats, (node) => node.name === node.name));
        if (stats.completed_at) {
          color = (stats.successful)
            ? '#5cb85c'  // Bootstrap's "success" style
            : '#d9534f'; // Bootstrap's "danger" style
        } else {
          color = '#f0ad4e'; // Bootstrap's "warning" style
        }
      }
      return Object.assign({name: node.name, op: node.op, color: color}, params);
    });
    const links = flatMap(this.props.graph, node => {
      return map(filter(node.inputs, (input) => input.reference), (input, key) => {
        const source = input.reference.substring(0, input.reference.indexOf("/"));
        const port = input.reference.substring(input.reference.indexOf("/") + 1);
        return {source, target: node.name, port};
      });
    });
    let allParams = flatMap(this.props.graph, node => filter(toPairs(node.inputs, (kv) => kv[1].value)).map(kv => 'in:' + kv[0]));
    allParams.unshift('op');

    const domNode = document.getElementById(this.props.id);
    while (domNode.firstChild) {
      domNode.removeChild(domNode.firstChild);
    }

    d3plus.viz()
      .container([[domNode]]) // Format produced by d3-select methods.
      .type('network')
      .data(nodes)
      .edges(links)
      .edges({arrows: true})
      .id('name')
      .color('color')
      .size(5)
      .format({text: identity})
      .tooltip({short: [], long: allParams})
      .draw();
  },

  componentDidMount: function () {
    this._updateGraph();
  },

  componentDidUpdate: function () {
    this._updateGraph();
  },

  render: function () {
    return <div id={this.props.id} style={{height: this.props.height}} className="run-graph"></div>;
  }
});

GraphView.propTypes = {
  graph: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
  run: React.PropTypes.object,
  height: React.PropTypes.number.isRequired
};

export default GraphView;
