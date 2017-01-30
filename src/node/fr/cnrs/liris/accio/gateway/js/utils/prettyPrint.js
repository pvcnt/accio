/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016-2017 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

import React from 'react'
import {isObject, isArray, toPairs, toString} from 'lodash'

function prettyPrint(obj) {
  if (isObject(obj)) {
    return '{' + toPairs(obj).map(kv => kv[0] + '=' + prettyPrint(kv[1])).join(', ') + '}'
  } else if (isArray(obj)) {
    return '[' + obj.map(v => prettyPrint(v)).join(', ') + ']'
  } else {
    return toString(obj)
  }
}

function prettyPrintKind(kind) {
  if (kind.base === 'list' || kind.base == 'set') {
    return kind.base + '(' + kind.args[0] + ')'
  } else if (kind.base === 'map') {
    return kind.base + '(' + kind.args[0] + ', ' + kind.args[1] + ')'
  } else {
    return kind.base
  }
}

function prettyPrintValue(value, kind) {
  if (kind.base === 'distance') {
    return value + ' meters'
  } else if (kind.base === 'duration') {
    return value + ' millis'
  } else if (kind.base === 'dataset') {
    return <a href={value.uri}>{value.uri}</a>
  } else {
    return prettyPrint(value)
  }
}

export {prettyPrint, prettyPrintKind, prettyPrintValue}
