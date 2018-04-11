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
import {forEach, values, some, uniqueId, find, noop} from "lodash";
import d3 from "d3";
import dagreD3 from 'dagre-d3';

/** Set of parameters that define the look and feel of the graph. */
const PARAMS = {
  graph: {
    /**
     * Dagre's nodesep param - number of pixels that
     * separate nodes horizontally in the layout.
     *
     * See https://github.com/cpettitt/dagre/wiki#configuring-the-layout
     */
    nodeSep: 5,
    /**
     * Dagre's ranksep param - number of pixels
     * between each rank in the layout.
     *
     * See https://github.com/cpettitt/dagre/wiki#configuring-the-layout
     */
    rankSep: 25,
    /**
     * Dagre's edgesep param - number of pixels that separate
     * edges horizontally in the layout.
     */
    edgeSep: 5,
  },
  /**
   * Padding is used to correctly position the graph SVG inside of its parent
   * element. The padding amounts are applied using an SVG transform of X and
   * Y coordinates.
   */
  padding: {
    paddingTop: 40,
    paddingLeft: 20,
  },
  nodeSize: {
    width: 120,
    height: 25,
    maxLabelWidth: 100,
  },
};

const createGraph = (workflow) => {
  const graph = new dagreD3.graphlib.Graph(PARAMS.graph);
  graph.setGraph({});
  workflow.nodes.forEach((node) => {
    forEach(node.inputs, (v) => {
      if (v.reference) {
        graph.setEdge(v.reference.node, node.name, {});
      }
    });
    const nodeParams = Object.assign({ defn: node }, PARAMS.nodeSize);
    if (node.name.length > PARAMS.nodeSize.maxLabelWidth / 5) {
      nodeParams.label = node.name.substring(0, PARAMS.nodeSize.maxLabelWidth / 5 - 3) + '...';
      nodeParams.title = node.name;
    } else {
      nodeParams.label = node.name;
    }
    graph.setNode(node.name, nodeParams);
  });

  return graph;
};

class InteractiveGraph extends React.Component {
  componentDidMount() {
    this.redraw();
  }

  componentDidUpdate() {
    this.redraw();
  }

  redraw() {
    const height = 700;
    const width = this.rootNode.parentNode.getBoundingClientRect().width - 30;
    d3.select(this.rootNode).select('svg').remove();

    const zoom = d3.behavior.zoom();
    const svg = d3.select(this.rootNode)
      .append('svg')
      .attr('width', width)
      .attr('height', height)
      .call(zoom);
    const inner = svg.append('g');
    zoom.on('zoom', () => {
      svg.attr('transform', 'translate(' + d3.event.translate + ') ' +
        'scale(' + d3.event.scale + ')');
    });

    const graph = createGraph(this.props.workflow);
    const renderer = new dagreD3.render();
    renderer(inner, graph);

    const initialScale = Math.min(height / graph.graph().height, width / graph.graph().width);
    zoom.scale(initialScale);
    svg.attr('transform', 'scale(' + initialScale + ')');

    inner.selectAll('g.node')
      .attr('title', d => graph.node(d).title)
      .on('click', d => this.props.onClick(graph.node(d).defn))
  }

  render() {
    return (
      <div className="workflow-graph" ref={node => this.rootNode = node}></div>
    );
  }
}

InteractiveGraph.propTypes = {
  workflow: React.PropTypes.object.isRequired,
  onClick: React.PropTypes.func.isRequired,
};
InteractiveGraph.defaultProps = {
  onClick: noop,
}

export default InteractiveGraph;
