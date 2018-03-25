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
import Plotly from 'react-plotlyjs'

class Plot extends React.Component {
  render() {
    let data = [];
    if ('pdf' === this.props.type || 'cdf' === this.props.type) {
      data.push({
        x: this.props.data.x,
        y: this.props.data.y,
        mode: 'lines',
        type: 'scatter',
      });
    } else {
      data.push({
        x: this.props.data,
        type: 'histogram',
      });
    }

    /*const ytitle = ('pdf' === this.props.type || 'cdf' === this.props.type)
      ? 'Proportion of elements'
      : 'Number of elements';*/
    const layout = {
      margin: {b: 30, t: 50, l: 30, r: 25},
      width: this.props.width,
      height: this.props.height,
      title: this.props.name,
      titlefont: {
        family: 'Helvetica Neue, Helvetica, Arial, sans-serif',
        size: 14,
      },
      xaxis: {title: this.props.unit},
      yaxis: {title: null},
    };
    const config = {
      showLink: false,
      displaylogo: false,
    };
    return <div><Plotly data={data} layout={layout} config={config}/></div>;
  }
}

Plot.propTypes = {
  data: React.PropTypes.oneOfType([React.PropTypes.object, React.PropTypes.array]),
  name: React.PropTypes.string.isRequired,
  unit: React.PropTypes.string,
  type: React.PropTypes.string.isRequired,
  width: React.PropTypes.number.isRequired,
  height: React.PropTypes.number.isRequired,
};

export default Plot;
