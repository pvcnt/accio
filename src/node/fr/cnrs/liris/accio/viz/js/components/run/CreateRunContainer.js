import React from "react";
import xhr from "../../utils/xhr";
import {Grid} from "react-bootstrap";
import CreateRunForm from "./CreateRunForm";
import Spinner from "react-spinkit";


let CreateRunContainer = React.createClass({
  getInitialState: function () {
    return {
      data: null,
    };
  },

  _loadData: function (props) {
    if (props.params.id) {
      xhr('/api/workflow/' + props.params.id).then(data => this.setState({data: data}));
    }
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
      ? <CreateRunForm workflow={this.state.data} {...this.props}/>
      : <Grid><Spinner spinnerName="three-bounce"/></Grid>;
  }
});

CreateRunContainer.propTypes = {
  params: React.PropTypes.shape({
    id: React.PropTypes.string.isRequired,
  })
};

export default CreateRunContainer;