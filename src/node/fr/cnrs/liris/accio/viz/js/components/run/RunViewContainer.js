import React from "react";
import {Grid} from "react-bootstrap";
import RunView from "./RunView";
import xhr from "../../utils/xhr";
import Spinner from "react-spinkit";

let RunViewContainer = React.createClass({
  getInitialState: function () {
    return {data: null};
  },

  _loadData: function (props) {
    xhr('/api/run/' + props.params.id).then(data => this.setState({data: data}));
  },

  componentWillReceiveProps: function (nextProps) {
    if (this.props.params.id !== nextProps.params.id) {
      this._loadData(nextProps);
    }
  },

  componentDidMount: function () {
    this._loadData(this.props);
  },

  render: function () {
    return (null !== this.state.data)
      ? <RunView run={this.state.data} {...this.props}/>
      : <Grid><Spinner spinnerName="three-bounce"/></Grid>;
  }
});

RunViewContainer.propTypes = {
  params: React.PropTypes.shape({
    id: React.PropTypes.string.isRequired,
  })
};

export default RunViewContainer;
