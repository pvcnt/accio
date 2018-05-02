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

function status(response) {
  if (response.status >= 200 && response.status < 300) {
    return response;
  } else {
    return Promise.reject(response);
  }
}

function json(response) {
  return response.json()
    .catch(() => {
      // This `catch` is here to handle the case where the content length is 0, in which case the
      // JSON deserialization fails with "Unexpected end of input". However, it seems impossible to
      // get the content length from the fetch response object (in particular the "Content-Length"
      // header is undefined). So we end up using this catch block, which is far from ideal as it
      // will also hide legitimate JSON deserialization errors...
      // Cf. https://stackoverflow.com/questions/48266678/how-to-get-the-content-length-of-the-response-from-a-request-with-fetch#comment83517087_48266842
      return {};
    }).then(data => {
      // We add the HTTP status code to help differentiate between errors.
      return { httpStatus: response.status, ...data };
    });
}

export default function xhr(url, params = {}) {
  const blob = !!params.blob;
  delete params.blob;
  params = {
    headers: { 'Content-Type': 'application/json' },
    credentials: 'same-origin',
    method: 'GET',
    ...params,
  };
  const accessToken = window.localStorage.getItem('access_token');
  if (null !== accessToken) {
    params.headers['Authorization'] = `Bearer ${accessToken}`;
  }
  const f = fetch(url, params).then(status);
  if (blob) {
    return f.then(resp => resp.blob());
  } else {
    return f.then(json, json);
  }
}