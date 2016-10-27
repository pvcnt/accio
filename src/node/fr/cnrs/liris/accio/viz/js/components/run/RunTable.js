import React from "React";
import moment from "moment";
import {Link} from "react-router";
import {ProgressBar, Table, Label} from "react-bootstrap";

let RunTable = React.createClass({
  render: function () {
    const rows = this.props.runs.map((run, idx) => {
      const style = (run.completed) ? (run.is_successful) ? 'success' : 'danger' : 'warning';
      const tags = (run.tags.length > 0) ? run.tags.map((tag, idx) => <Label key={idx}>tag</Label>) : '–';
      return (
        <tr key={idx}>
          <td><input type="checkbox" /></td>
          <td>
            <Link to={'/runs/view/' + run.id}>{run.name}</Link>
          </td>
          <td>{run.workflow}</td>
          <td>{run.owner.name}</td>
          <td><ProgressBar now={run.progress * 100} label={(run.progress * 100) + '%'} bsStyle={style}/></td>
          <td>{(run.started) ? moment(run.started).fromNow() : '–'}</td>
          <td>{tags}</td>
        </tr>
      );
    });
    return (
      <Table striped hover responsive className="accio-list-table">
        <thead>
        <tr>
          <th></th>
          <th>Run name</th>
          <th>Workflow</th>
          <th>Owner</th>
          <th>Progress</th>
          <th>Started</th>
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

RunTable.propTypes = {
  runs: React.PropTypes.array.isRequired,
};

export default RunTable;
