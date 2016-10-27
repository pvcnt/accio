import React from "React";
import {Panel} from "react-bootstrap";

let LazyPanel = React.createClass({
  getDefaultProps: function () {
    return {
      collapsible: false
    };
  },

  getInitialState: function () {
    return {
      expanded: !this.props.collapsible || this.props.defaultExpanded || this.props.expanded
    }
  },

  _handleEnter: function () {
    this.setState({expanded: true});
  },

  _handleExited: function () {
    this.setState({expanded: false});
  },

  componentWillReceiveProps: function (nextProps) {
    const expanded = !nextProps.collapsible || nextProps.expanded;
    if (expanded != this.state.expanded) {
      this.setState({expanded: expanded});
    }
  },

  render: function () {
    return (
      <Panel {...this.props} onEnter={this._handleEnter} onExited={this._handleExited}>
        {this.state.expanded ? this.props.children : null}
      </Panel>
    );
  }
});

export default LazyPanel;