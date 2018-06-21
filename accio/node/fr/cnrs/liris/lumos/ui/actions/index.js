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

import xhr from '../utils/xhr';
import { JOBS_PER_PAGE, COMPLETED_STATES } from '../constants';

export const FETCH_JOBS_REQUEST = 'FETCH_JOBS_REQUEST';
export const FETCH_JOBS_SUCCESS = 'FETCH_JOBS_SUCCESS';
export const FETCH_JOBS_FAILED = 'FETCH_JOBS_FAILED';

export const GET_JOB_REQUEST = 'GET_JOB_REQUEST';
export const GET_JOB_SUCCESS = 'GET_JOB_SUCCESS';
export const GET_JOB_FAILED = 'GET_JOB_FAILED';

function isJobTerminated(job) {
  return COMPLETED_STATES.indexOf(job.status.state) > -1;
}

export function fetchJobs(page = 1, labelSelector = null) {
  return (dispatch) => {
    dispatch({ type: FETCH_JOBS_REQUEST, page, labelSelector });

    let url = `/api/v1/jobs?page=${page}&perPage=${JOBS_PER_PAGE}`;
    if (null !== labelSelector) {
      url += `&labels=${labelSelector}`;
    }
    return xhr(url).then(
      resp => dispatch({ type: FETCH_JOBS_SUCCESS, jobs: resp.jobs, totalCount: resp.totalCount }),
      resp => {
        console.error('Unexpected error while fetching jobs.');
        dispatch({ type: FETCH_JOBS_FAILED, page });
      }
    );
  }
}

export function getJob(name) {
  return (dispatch, getState) => {
    const state = getState();
    if (state.jobs.status[name] === 'loaded' && isJobTerminated(state.jobs.entities[name])) {
      return Promise.resolve();
    }
    dispatch({ type: GET_JOB_REQUEST, name });

    return xhr(`/api/v1/jobs/${name}`).then(
      resp => dispatch({ type: GET_JOB_SUCCESS, job: resp }),
      resp => {
        console.error(`Unexpected error while fetching job ${name}.`);
        dispatch({ type: GET_JOB_FAILED, name });
      }
    );
  }
}