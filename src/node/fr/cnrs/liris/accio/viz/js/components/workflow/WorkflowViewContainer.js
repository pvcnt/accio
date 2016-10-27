import React from "react";
import {Grid} from "react-bootstrap";
import WorkflowView from "./WorkflowView";
import xhr from "../../utils/xhr";
import Spinner from "react-spinkit";

let WorkflowViewContainer = React.createClass({
  getInitialState: function () {
    return {data: null};
  },

  _loadData: function (props) {
    const url = '/api/workflow/' + props.params.id + (props.params.version ? '?version=' + props.params.version : '');
    xhr(url)
      .then(data => this.setState({data: data}));
  },

  componentWillReceiveProps: function (nextProps) {
    if (this.props.params.id !== nextProps.params.id || this.props.params.version !== nextProps.params.version) {
      this._loadData(nextProps);
    }
  },

  componentDidMount: function () {
    this._loadData(this.props);
  },

  render: function () {
    return (null !== this.state.data)
      ? <WorkflowView workflow={this.state.data} {...this.props}/>
      : <Grid><Spinner spinnerName="three-bounce"/></Grid>;
  }
});

WorkflowViewContainer.propTypes = {
  params: React.PropTypes.shape({
    id: React.PropTypes.string.isRequired,
    version: React.PropTypes.string,
  })
};

export default WorkflowViewContainer;
