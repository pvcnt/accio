import {
  FETCH_JOBS_REQUEST,
  FETCH_JOBS_SUCCESS,
  FETCH_JOBS_FAILED,
  GET_JOB_FAILED,
  GET_JOB_REQUEST,
  GET_JOB_SUCCESS,
} from '../actions';

const initialState = {
  jobs: {
    status: {},
    entities: {}
  },
  jobList: {
    status: 'pending',
    totalCount: 0,
    entities: [],
  },
};

function updateJobStatus(state, names, status) {
  const statusPatch = {};
  names.forEach(name => statusPatch[name] = status);
  return {
    ...state,
    jobs: {
      ...state.jobs,
      status: { ...state.jobs.status, patch: statusPatch },
    },
  };
}

function updateJobEntities(state, entities) {
  const statusPatch = {};
  const entitiesPatch = {};
  entities.forEach(entity => {
    statusPatch[entity.name] = 'loaded';
    entitiesPatch[entity.name] = entity;
  });
  return {
    ...state,
    jobs: {
      ...state.jobs,
      status: { ...state.jobs.status, patch: statusPatch },
      entities: { ...state.jobs.status, patch: entitiesPatch },
    },
  };
}

function updateJobListStatus(state, status) {
  return {
    ...state,
    jobList: {
      ...state.jobList,
      status,
    },
  };
}

function updateJobListEntities(state, entities, totalCount) {
  return {
    ...state,
    jobList: {
      ...state.jobList,
      totalCount,
      status: 'loaded',
      entities: entities.map(job => job.name),
    },
  };
}

export default function rootReducer(state = initialState, action) {
  switch (action.type) {
    case GET_JOB_REQUEST:
      return updateJobStatus(state, [action.name], 'loading');
    case GET_JOB_SUCCESS:
      return updateJobEntities(state, [action.job]);
    case GET_JOB_FAILED:
      return updateJobStatus(state, [action.job.name], 'failed');
    case FETCH_JOBS_REQUEST:
      return updateJobListStatus(state, 'loading');
    case FETCH_JOBS_SUCCESS:
      return updateJobListEntities(updateJobEntities(state, action.jobs), action.jobs, action.totalCount);
    case FETCH_JOBS_FAILED:
      return updateJobListStatus(state, 'failed');
    default:
      return state;
  }
}