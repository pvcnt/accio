import React from "React";
import d3plus from "d3plus";
import {uniqueId} from "lodash";
import {map, flatMap, mapKeys, keys, identity, filter, head, uniqid} from "lodash";

let GraphView = React.createClass({
  getDefaultProps: function () {
    return {
      id: uniqueId(),
    };
  },

  _updateGraph: function () {
    const nodes = this.props.graph.nodes.map(node => {
      const params = mapKeys(node.params, (value, key) => 'param:' + key);
      let color = '#5bc0de'; // Bootstrap's "info" style
      if (this.props.run) {
        const stats = head(filter(this.props.run.per_node, (node) => node.name === node.name));
        if (stats.completed) {
          color = (stats.is_successful)
            ? '#5cb85c'  // Bootstrap's "success" style
            : '#d9534f'; // Bootstrap's "danger" style
        } else {
          color = '#f0ad4e'; // Bootstrap's "warning" style
        }
      }
      return Object.assign({name: node.name, op: node.op, color: color}, params);
    });
    const links = flatMap(this.props.graph.nodes, node => {
      return map(node.inputs, (value, key) => {
        const source = value.substring(0, value.indexOf("/"));
        return {source: source, target: node.name};
      });
    });
    let allParams = flatMap(this.props.graph.nodes, node => keys(node.params)).map(key => 'param:' + key);
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
  graph: React.PropTypes.object.isRequired,
  run: React.PropTypes.object,
  height: React.PropTypes.number.isRequired
};

export default GraphView;
