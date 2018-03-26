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
import d3plus from "d3plus";
import {map, flatMap, fromPairs, toPairs, keys, identity, filter, head, uniqueId, uniq} from "lodash";
import {prettyPrintValue} from '../../utils/prettyPrint';

let GraphView = React.createClass({
  getDefaultProps: function () {
    return {
      id: uniqueId(),
    };
  },

  _updateGraph: function () {
    const nodes = this.props.graph.map(node => {
      const params = fromPairs(filter(toPairs(node.inputs), (kv) => !kv[1].reference).map(kv => {
        if (kv[1].value) {
          return ['in:' + kv[0], prettyPrintValue(kv[1].value.payload, kv[1].value.kind)];
        } else if (kv[1].param) {
          return ['in:' + kv[0], '$' + kv[1].param];
        } else {
          // Shouldn't be here.
          return kv
        }
      }));
      let color = '#5bc0de'; // Bootstrap's "info" style
      /*if (this.props.report) {
        const stats = head(filter(this.props.report.node_stats, (node) => node.name === node.name));
        if (stats.completed_at) {
          color = (stats.successful)
            ? '#5cb85c'  // Bootstrap's "success" style
            : '#d9534f'; // Bootstrap's "danger" style
        } else {
          color = '#f0ad4e'; // Bootstrap's "warning" style
        }
      }*/
      return Object.assign({name: node.name, op: node.op, color: color}, params);
    });
    const links = flatMap(this.props.graph, node => {
      return map(filter(node.inputs, (input) => input.reference), input => {
        return {source: input.reference.node, target: node.name, port: input.reference.port};
      });
    });
    let allParams = uniq(flatMap(this.props.graph, node => toPairs(node.inputs).map(kv => 'in:' + kv[0])));
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
