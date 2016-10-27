import React from "react";
import Plotly from 'react-plotlyjs';

let ScalarSummary = React.createClass({
  render: function () {
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
    const title = ('pdf' === this.props.type)
      ? 'Probability distribution of ' + this.props.name
      : ('cdf' === this.props.type)
      ? 'Cumulative distribution of ' + this.props.name
      : 'Values taken by of ' + this.props.name;
    /*const ytitle = ('pdf' === this.props.type || 'cdf' === this.props.type)
      ? 'Proportion of elements'
      : 'Number of elements';*/
    const layout = {
      margin: {b: 20, t: 50, l: 25, r: 25},
      width: this.props.width,
      height: this.props.height,
      title: title,
      titlefont: {
        family: 'Helvetica Neue, Helvetica, Arial, sans-serif',
        size: 14,
      },
      xaxis: {title: null},
      yaxis: {title: null},
    };
    const config = {
      showLink: false,
      displaylogo: false,
    };
    const plot = <Plotly data={data} layout={layout} config={config}/>;
    return (
      <div>{plot}</div>
    );
  }
});

ScalarSummary.propTypes = {
  data: React.PropTypes.oneOfType([React.PropTypes.object, React.PropTypes.array]),
  name: React.PropTypes.string.isRequired,
  type: React.PropTypes.string.isRequired,
  width: React.PropTypes.number.isRequired,
  height: React.PropTypes.number.isRequired,
};

export default ScalarSummary;
