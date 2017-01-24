import React from "react";

let RunSection = React.createClass({
  render: function () {
    return <div>{this.props.children}</div>;
  }
});

export default RunSection;