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

import React from "react";
import PlotArtifactGroup from '../artifact/PlotArtifactGroup'
import RawArtifactGroup from '../artifact/RawArtifactGroup'
import {map} from 'lodash'

const NUMERICS = ['byte', 'integer', 'double', 'distance', 'duration']

function _isNumeric(kind) {
  if (NUMERICS.indexOf(kind.base) > -1) {
    return true
  } else if (kind.base == 'list' || kind.base == 'set') {
    return _isNumeric({base: kind.args[0]})
  } else if (kind.base == 'map') {
    return _isNumeric({base: kind.args[1]})
  }
}

function _detectGroup(artifact) {
  if (_isNumeric(artifact.kind)) {
    return 'plot'
  }
  return 'raw'
}

class RunArtifacts extends React.Component {
  render() {
    const groups = {}
    this.props.artifacts.forEach(artifact => {
      const group = _detectGroup(artifact)
      if (!groups[group]) {
        groups[group] = []
      }
      groups[group].push(artifact)
    })
    const elements = map(groups, (artifacts, type) => {
      if (type == 'plot') {
        return <PlotArtifactGroup key={type} artifacts={artifacts}/>
      } else if (type == 'raw') {
        return <RawArtifactGroup key={type} artifacts={artifacts}/>
      }
    })
    return <div>{elements}</div>
  }
}

RunArtifacts.propTypes = {
  nodeName: React.PropTypes.string.isRequired,
  artifacts: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
};

export default RunArtifacts;
