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

export const JOBS_PER_PAGE = 30;

export const FAILED_STATES = ['Lost', 'Failed', 'Canceled'];
export const SUCCESSFUL_STATES = ['Successful'];
export const RUNNING_STATES = ['Running'];
export const COMPLETED_STATES = FAILED_STATES.concat(SUCCESSFUL_STATES);

export function isJobFailed(job) {
  return FAILED_STATES.indexOf(job.status.state) > -1;
}

export function isJobRunning(job) {
  return RUNNING_STATES.indexOf(job.status.state) > -1;
}

export function isJobSuccessful(job) {
  return SUCCESSFUL_STATES.indexOf(job.status.state) > -1;
}

export function isJobCompleted(job) {
  return COMPLETED_STATES.indexOf(job.status.state) > -1;
}