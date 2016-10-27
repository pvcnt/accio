import React from "react";

let WorkflowSection = React.createClass({
  render: function () {
    return <div>{this.props.children}</div>;
  }
});

export default WorkflowSection;