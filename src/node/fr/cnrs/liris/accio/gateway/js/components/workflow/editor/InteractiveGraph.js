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
import React from "react";
import klay from "klayjs-d3";
import {forEach, values, some, uniqueId, find, noop} from "lodash";
import d3 from "d3";

const createGraph = (workflow) => {
  const nodes = {};
  const links = [];
  workflow.graph.forEach((node) => {
    const ports = [];
    forEach(node.inputs, (v, k) => {
      if (v.reference) {
        const portName = node.name + '.in.' + k;
        ports.push({
          id: portName,
          properties: {
            'de.cau.cs.kieler.portSide': 'NORTH',
          },
        });
        links.push({
          id: v.reference.node + '.out.' + v.reference.port + '->' + portName,
          source: v.reference.node,
          target: node.name,
          sourcePort: v.reference.node + '.out.' + v.reference.port,
          targetPort: portName,
        });
      }
    });
    nodes[node.name] = {
      id: node.name,
      width: 100,
      height: 25,
      labels: [{text: node.name}],
      ports,
      properties: {
        'de.cau.cs.kieler.portAlignment': 'END',
        'de.cau.cs.kieler.portConstraints': 'FIXED_POS',
      }
    };
  });
  workflow.graph.forEach((node) => {
    forEach(node.inputs, (v, k) => {
      if (v.reference) {
        const portName = v.reference.node + '.out.' + v.reference.port;
        const ports = nodes[v.reference.node].ports;
        if (!some(ports, port => port.id === portName)) {
          ports.push({
            id: portName,
            properties: {
              'de.cau.cs.kieler.portSide': 'SOUTH',
            },
          });
        }
      }
    });
  });

  return {nodes: values(nodes), links};
};

class InteractiveGraph extends React.Component {
  componentDidMount() {
    this.redraw();
  }

  componentDidUpdate() {
    this.redraw();
  }

  redraw() {
    const graph = createGraph(this.props.workflow);
    const height = 1000;
    const options = {
      intCoordinates: true,
      algorithm: 'de.cau.cs.kieler.klay.layered',
      layoutHierarchy: true,
      spacing: 20,
      borderSpacing: 20,
      edgeSpacingFactor: 0.2,
      inLayerSpacingFactor: 2.0,
      nodePlace: 'BRANDES_KOEPF',
      nodeLayering: 'NETWORK_SIMPLEX',
      edgeRouting: 'POLYLINE',
      direction: 'DOWN',
    };

    const width = d3.select('#' + this.id).node().parentNode.getBoundingClientRect().width;
    const zoom = d3.behavior.zoom();
    const svg = d3.select('#' + this.id)
      .append('svg')
      .attr('width', width)
      .attr('height', height)
      .call(zoom)
      .append('g');

    zoom.on('zoom', () => {
      svg.attr('transform', 'translate(' + d3.event.translate + ')'
        + ' scale(' + d3.event.scale + ')');
    });

    // define an arrow head
    svg.append('svg:defs')
      .append('svg:marker')
      .attr('id', 'end')
      .attr('viewBox', '0 -5 10 10')
      .attr('refX', 10)
      .attr('refY', 0)
      .attr('markerWidth', 3)        // marker settings
      .attr('markerHeight', 5)
      .attr('orient', 'auto')
      .style('fill', '#999')
      .style('stroke-opacity', 0.6)  // arrowhead color
      .append('svg:path')
      .attr('d', 'M0,-5L10,0L0,5');

    // group
    const root = svg.append('g');
    const layouter = klay.d3kgraph()
      .size([width, height])
      .transformGroup(root)
      .options(options);

    layouter.on('finish', () => {
      const nodes = layouter.nodes();
      const links = layouter.links(nodes);

      // #1 add the nodes' groups
      const nodeData = root.selectAll('.node')
        .data(nodes, function (d) {
          return d.id;
        });

      const node = nodeData.enter()
        .append('g')
        .attr('class', function (d) {
          if (d.children)
            return 'node compound';
          else
            return 'node leaf';
        })
        .on('click', () => {
          d3.event.stopPropagation();
          const node = find(this.props.workflow.graph, node => node.name == d3.event.target.__data__.id);
          this.props.onChange(node);
        });

      // add representing boxes for nodes
      node.append('rect')
        .attr('class', 'atom')
        .attr('width', 0)
        .attr('height', 0);

      // add node labels
      node.append('text')
        .attr('x', 2.5)
        .attr('y', 6.5)
        .text(function (d) {
          return d.id;
        })
        .attr('font-size', '4px');


      // #2 add paths with arrows for the edges
      const linkData = root.selectAll('.link')
        .data(links, function (d) {
          return d.id;
        });
      const link = linkData.enter()
        .append('path')
        .attr('class', 'link')
        .attr('d', 'M0 0')
        .attr('marker-end', 'url(#end)');

      // #3 update positions of all elements

      // node positions
      nodeData.transition()
        .attr('transform', function (d) {
          return 'translate(' + (d.x || 0) + ' ' + (d.y || 0) + ')';
        });
      // node sizes
      nodeData.select('.atom')
        .transition()
        .attr('width', function (d) {
          return d.width;
        })
        .attr('height', function (d) {
          return d.height;
        });

      // edge routes
      linkData.transition().attr('d', function (d) {
        let path = '';
        if (d.sourcePoint && d.targetPoint) {
          path += 'M' + d.sourcePoint.x + ' ' + d.sourcePoint.y + ' ';
          (d.bendPoints || []).forEach(function (bp, i) {
            path += 'L' + bp.x + ' ' + bp.y + ' ';
          });
          path += 'L' + d.targetPoint.x + ' ' + d.targetPoint.y + ' ';
        }
        return path;
      });

    });

    layouter.kgraph({id: 'root', edges: graph.links, children: graph.nodes});
  }

  render() {
    this.id = uniqueId('editor');
    return (
      <div id={this.id}></div>
    );
  }
}

InteractiveGraph.propTypes = {
  workflow: React.PropTypes.object.isRequired,
  onChange: React.PropTypes.func.isRequired,
};
InteractiveGraph.defaultProps = {
  onChange: noop,
}

export default InteractiveGraph;
