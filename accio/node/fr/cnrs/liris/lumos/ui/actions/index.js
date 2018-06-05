import xhr from '../utils/xhr';

export const FETCH_JOBS_REQUEST = 'FETCH_JOBS_REQUEST';
export const FETCH_JOBS_SUCCESS = 'FETCH_JOBS_SUCCESS';
export const FETCH_JOBS_FAILED = 'FETCH_JOBS_FAILED';

export const GET_JOB_REQUEST = 'GET_JOB_REQUEST';
export const GET_JOB_SUCCESS = 'GET_JOB_SUCCESS';
export const GET_JOB_FAILED = 'GET_JOB_FAILED';

const JOBS_PER_PAGE = 30;

const TERMINAL_STATES = ['Successful', 'Failed', 'Canceled', 'Lost'];

function isJobTerminated(job) {
  return TERMINAL_STATES.indexOf(job.status.state) > -1;
}

export function fetchJobs(page, labelSelector = null) {
  return (dispatch) => {
    dispatch({ type: FETCH_JOBS_REQUEST, page });

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