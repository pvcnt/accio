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

import {
  FETCH_JOBS_REQUEST,
  FETCH_JOBS_SUCCESS,
  FETCH_JOBS_FAILED,
  GET_JOB_FAILED,
  GET_JOB_REQUEST,
  GET_JOB_SUCCESS,
} from '../actions';

const initialState = {
  status: {},
  entities: {},
  list: {
    status: 'pending',
    totalCount: 0,
    page: 1,
    labelSelector: null,
    ids: [],
  },
};

function updateJobStatus(state, names, status) {
  const statusPatch = {};
  names.forEach(name => statusPatch[name] = status);
  return {
    ...state,
    status: { ...state.status, ...statusPatch },
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
    status: { ...state.status, ...statusPatch },
    entities: { ...state.entities, ...entitiesPatch },
  };
}

function updateJobListEntities(state, entities, totalCount) {
  return {
    ...state,
    list: {
      ...state.list,
      totalCount,
      status: 'loaded',
      ids: entities.map(job => job.name),
    },
  };
}

export default function jobsReducer(state = initialState, action) {
  switch (action.type) {
    case GET_JOB_REQUEST:
      return updateJobStatus(state, [action.name], 'loading');
    case GET_JOB_SUCCESS:
      return updateJobEntities(state, [action.job]);
    case GET_JOB_FAILED:
      return updateJobStatus(state, [action.job.name], 'failed');
    case FETCH_JOBS_REQUEST:
      return {
        ...state,
        list: {
          ...state.list,
          status: 'loading',
          page: action.page,
          labelSelector: action.labelSelector,
        },
      };
    case FETCH_JOBS_SUCCESS:
      return updateJobListEntities(updateJobEntities(state, action.jobs), action.jobs, action.totalCount);
    case FETCH_JOBS_FAILED:
      return {
        ...state,
        list: { ...state.list, status: 'failed' },
      };
    default:
      return state;
  }
}