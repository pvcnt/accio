/*
 * Accio is a platform to launch computer science experiments.
 * Copyright (C) 2016-2018 Vincent Primault <v.primault@ucl.ac.uk>
 *
 * Accio is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Accio is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Accio.  If not, see <http://www.gnu.org/licenses/>.
 */

import React from 'react';
import PropTypes from 'prop-types';
import { withRouter } from 'react-router-dom';
import { toPairs, findLastIndex }  from 'lodash';
import { Tag, Intent, ProgressBar } from '@blueprintjs/core';
import moment from 'moment';

import { isJobFailed, isJobSuccessful, isJobRunning } from '../../constants';

@withRouter
class JobTable extends React.Component {
  handleClick(job) {
    this.props.history.push(`/jobs/view/${job.name}`);
  }

  render() {
    const rows = this.props.jobs.map((job, idx) => {
      const labels = toPairs(job.labels).map((kv, idx) => {
        return <Tag intent={Intent.PRIMARY} key={idx}>{kv[0]}={kv[1]}</Tag>;
      });

      let startTime = job.createTime;
      if (job.status.state === 'Running') {
        startTime = job.status.time;
      } else {
        const idx = findLastIndex(job.history, item => item.state === 'Running');
        if (idx > -1) {
          startTime = job.history[idx].time;
        }
      }

      let progressBar;
      let label = job.status.state;
      if (isJobFailed(job)) {
        progressBar = <ProgressBar value={1} intent={Intent.DANGER} animate={false} stripes={false}/>;
      } else if (isJobSuccessful(job)) {
        progressBar = <ProgressBar value={1} intent={Intent.SUCCESS} animate={false} stripes={false}/>;
        if (isJobRunning(job)) {
          label = `${job.progress} %`;
        }
      } else {
        progressBar = <ProgressBar value={job.progress / 100} stripes={false}/>;
      }

      return (
        <tr onClick={() => this.handleClick(job)} key={idx}>
          <td><input type="checkbox"/></td>
          <td>{job.name}</td>
          <td>{job.owner || '(unknown)'}</td>
          <td>
            {progressBar}
            <div className="progress-label">{label}</div>
          </td>
          <td>{moment(startTime).fromNow()}</td>
          <td>{labels}</td>
        </tr>
      );
    });

    return (
      <table className="pt-html-table pt-interactive pt-html-table-striped"
             style={{ width: '100%' }}>
        <thead>
        <tr>
          <th>&nbsp;</th>
          <th>ID</th>
          <th>Owner</th>
          <th>Progress</th>
          <th>Start Time</th>
          <th>Labels</th>
        </tr>
        </thead>
        <tbody>{rows}</tbody>
      </table>
    );
  }
}

JobTable.propTypes = {
  jobs: PropTypes.array.isRequired,
};

JobTable.defaultProps = {
  jobs: [],
};

export default JobTable;