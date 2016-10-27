import React from "React";
import {noop} from "lodash";
import {Row, Col, Button, FormControl, InputGroup, Glyphicon} from "react-bootstrap";

let RunFilter = React.createClass({
  getInitialState: function () {
    return {
      value: this.props.defaultValue,
    };
  },

  getDefaultProps: function () {
    return {
      defaultValue: '',
      onSubmit: noop
    };
  },

  _handleSubmit: function (e) {
    e.nativeEvent.preventDefault();
    this.props.onSubmit(this.state.value);
  },

  _handleChange: function (e) {
    this.setState({value: e.target.value});
  },

  render: function () {
    return (
      <form onSubmit={this._handleSubmit} className="accio-list-filter">
        <InputGroup>
          <FormControl type="text"
                      value={this.state.value}
                      onChange={this._handleChange}
                      placeholder="Search by name, workflow, owner or tags..."/>
          <InputGroup.Button>
            <Button type="submit"><Glyphicon glyph="search" /></Button>
          </InputGroup.Button>
        </InputGroup>
      </form>
    );
  }
});

RunFilter.propTypes = {
  defaultValue: React.PropTypes.string.isRequired,
  onSubmit: React.PropTypes.func.isRequired,
};

export default RunFilter;
