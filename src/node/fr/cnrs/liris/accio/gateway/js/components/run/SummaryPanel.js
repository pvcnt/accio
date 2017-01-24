import React from "react";
import {map, noop, concat, without} from "lodash";
import {Row, Col, ButtonGroup, Button, Glyphicon} from "react-bootstrap";
import ScalarSummary from "./ScalarSummary";

let SummaryPanel = React.createClass({
  getInitialState: function() {
    return {
      selected: [],
    };
  },

  getInitialProps: function () {
    return {
      onChange: noop,
    };
  },

  _handleChange: function (eventKey) {
    this.props.onChange(eventKey);
  },

  _handleSelect: function(e, name) {
    e.nativeEvent.preventDefault();
    if (this._isSelected(name)) {
      this.setState({selected: without(this.state.selected, name)});
    } else {
      this.setState({selected: concat(this.state.selected, name)});
    }
  },

  _isSelected: function (name) {
    return this.state.selected.indexOf(name) > -1;
  },

  render: function () {
    const cols = map(this.props.artifacts, (data, name) => {
      const isSelected = this._isSelected(name);
      return (
        <Col md={(isSelected) ? 12 : 4} key={name}>
          <ScalarSummary key={name}
                         name={name}
                         data={data}
                         type={this.props.type}
                         width={(isSelected) ? 1110 : 350}
                         height={(isSelected) ? 500 : 350}/>
          <a href="#" onClick={e => this._handleSelect(e, name)} className="accio-summary-resize">
            <Glyphicon glyph={(isSelected) ? 'resize-small' : 'resize-full'} />
          </a>
        </Col>
      );
    });

    return (
      <div>
        <div className="center">
          <ButtonGroup>
            <Button active={'points' === this.props.type}
                    onClick={() => this._handleChange('points')}>Histogram</Button>
            <Button active={'pdf' === this.props.type} onClick={() => this._handleChange('pdf')}>PDF</Button>
            <Button active={'cdf' === this.props.type} onClick={() => this._handleChange('cdf')}>CDF</Button>
          </ButtonGroup>
        </div>
        <Row>{cols}</Row>
      </div>
    );
  }
});

SummaryPanel.propTypes = {
  artifacts: React.PropTypes.object.isRequired,
  type: React.PropTypes.string.isRequired,
  onChange: React.PropTypes.func.isRequired,
};

export default SummaryPanel;
