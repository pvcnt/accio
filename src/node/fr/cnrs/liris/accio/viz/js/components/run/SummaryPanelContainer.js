import React from "react";
import {isEqual} from "lodash";
import {Grid} from "react-bootstrap";
import Spinner from "react-spinkit";
import SummaryPanel from "./SummaryPanel";
import xhr from "../../utils/xhr";

let SummaryPanelContainer = React.createClass({
  getInitialState: function () {
    return {
      data: null,
      type: 'points',
    };
  },

  _loadData: function (props, state) {
    xhr('/api/run/' + props.run + '/' + props.node + '/scalars?type=' + state.type)
      .then(data => this.setState(Object.assign({}, state, {data: data})));
  },

  _handleChange: function (type) {
    this._loadData(this.props, {type: type});
  },

  componentWillReceiveProps: function (nextProps) {
    if (!isEqual(this.props, nextProps)) {
      this._loadData(this.state, nextProps);
    }
  },

  componentDidMount: function () {
    this._loadData(this.props, this.state);
  },

  render: function () {
    return (null !== this.state.data)
      ? <SummaryPanel artifacts={this.state.data}
                        type={this.state.type}
                        onChange={this._handleChange}
                        {...this.props}/>
      : <Grid><Spinner spinnerName="three-bounce"/></Grid>;
  }
});

SummaryPanelContainer.propTypes = {
  run: React.PropTypes.string.isRequired,
  node: React.PropTypes.string.isRequired,
};

export default SummaryPanelContainer;
