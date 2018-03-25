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

import {noop} from 'lodash'

const status = (response) => {
  if (response.status >= 200 && response.status < 300) {
    return Promise.resolve(response);
  } else {
    return Promise.reject(new Error(response.statusText));
  }
};

const json = (response) => {
  if (response.status !== 204) {
    return response.json();
  } else {
    return {};
  }
};

const xhr = (url, params = {}, decodeJson = true) => {
  params = Object.assign({
    headers: {'Content-Type': 'application/json'},
    credentials: 'same-origin',
    method: 'GET',
  }, params)
  const f = fetch(url, params).then(status)
  return makeCancelable(decodeJson ? f.then(json) : f)
};


const makeCancelable = (promise) => {
  let hasCanceled_ = false

  function wrap(promise) {
    let richPromise = new Promise((resolve, reject) => {
      promise.then((val) => {
        if (!hasCanceled_) {
          resolve(val)
        }
      })
      promise.catch((error) => {
        if (!hasCanceled_) {
          reject(error)
        }
      })
    })
    richPromise.then = function (onFulfilled, onRejected) {
      return wrap(promise.then(onFulfilled, onRejected))
    }
    richPromise.catch = function (onRejected) {
      return wrap(promise.catch(onRejected))
    }
    richPromise.cancel = function () {
      hasCanceled_ = true
    }
    return richPromise
  }

  return wrap(promise)
}

export default xhr;
