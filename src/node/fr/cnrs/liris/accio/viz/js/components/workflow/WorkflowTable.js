import React from "React";
import moment from "moment";
import {Link} from "react-router";
import {Table} from "react-bootstrap";

let WorkflowTable = React.createClass({
  render: function () {
    const rows = this.props.workflows.map((workflow, idx) => {
      const tags = (workflow.tags.length > 0) ? workflow.tags.map((tag, idx) => <Label key={idx}>tag</Label>) : '–';
      return (
        <tr key={idx}>
          <td>
            <Link to={'/workflows/view/' + workflow.id}>
              {workflow.name}
            </Link>
          </td>
          <td>{workflow.owner.name}</td>
          <td>{moment(workflow.created).fromNow()}</td>
          <td></td>
          <td></td>
          <td>{tags}</td>
        </tr>
      );
    });
    return (
        <Table striped hover responsive className="accio-list-table">
          <thead>
          <tr>
            <th>Workflow</th>
            <th>Owner</th>
            <th>Created</th>
            <th>#runs</th>
            <th>Last run</th>
            <th>Tags</th>
          </tr>
          </thead>
          <tbody>
          {rows}
          </tbody>
        </Table>
    );
  }
});

WorkflowTable.propTypes = {
  workflows: React.PropTypes.array.isRequired,
};

export default WorkflowTable;
