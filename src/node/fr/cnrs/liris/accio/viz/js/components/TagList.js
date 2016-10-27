import React from "React";
import {Label} from "react-bootstrap";

let TagList = React.createClass({
  getDefaultProps: function () {
    return {
      tags: [],
    };
  },

  render: function () {
    const tags = (this.props.tags.length > 0)
      ? this.props.tags.map((tag, idx) => <Label key={idx}>tag</Label>)
      : '–';

    return <div>{tags}</div>;
  }
});

TagList.propTypes = {
  tags: React.PropTypes.arrayOf(React.PropTypes.string)
};

export default TagList;
