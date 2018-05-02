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

import xhr from './xhr';

export function checkAuthenticated() {
  return xhr('/auth').then(resp => resp.authenticated);
}

export function authenticate(password) {
  return xhr('/auth', { method: 'POST', body: JSON.stringify({ password }) }).then(resp => {
    if (resp.authenticated) {
      if (resp.accessToken) {
        window.localStorage.setItem('access_token', resp.accessToken);
      }
      return true;
    } else {
      return false;
    }
  });
}

export function logout() {
  window.localStorage.removeItem('access_token');
}