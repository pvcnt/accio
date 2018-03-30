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

import React from 'react'
import {map, noop, concat, without, values, min, max, range} from 'lodash'
import {Row, Col, ButtonGroup, Button, Glyphicon} from 'react-bootstrap'
//import Plot from './Plot'
import autobind from 'autobind-decorator'

const STEPS = 100

function _count(points, from, to) {
  let c = 0
  points.forEach(v => {
    if (v > from && v <= to) {
      c++
    }
  })
  return c
}

function _cdf(points, steps) {
  if (points.length == 0) {
    return []
  }
  const minValue = min(points)
  const maxValue = max(points)
  if (minValue == maxValue) {
    return [{x: minValue, y: 1}]
  }
  const by = (maxValue - minValue) / steps
  const n = points.length
  let x = [minValue]
  let y = [_count(points, minValue - 1, minValue) / n]
  range(minValue, maxValue, by).forEach(to => {
    x.push(to)
    y.push(_count(points, minValue, to) / n)
  })
  x.push(maxValue)
  y.push(1)
  return {x, y}
}

function _pdf(points, steps) {
  if (points.length == 0) {
    return []
  }
  const minValue = min(points)
  const maxValue = max(points)
  if (minValue == maxValue) {
    return [{x: minValue, y: points.length}]
  }
  const by = (maxValue - minValue) / steps
  let x = [minValue]
  let y = [_count(points, minValue - 1, minValue)]
  range(minValue, maxValue, by).forEach(to => {
    x.push(to)
    y.push(_count(points, to - by, to))
  })
  x.push(maxValue)
  y.push(_count(points, maxValue - by, maxValue))
  return {x, y}
}

class PlotArtifactGroup extends React.Component {
  constructor(props) {
    super(props)
    this.state = {type: 'pdf', selected: []}
  }

  @autobind
  _handleChange(type) {
    this.setState({type})
  }

  @autobind
  _handleSelect(e, name) {
    e.nativeEvent.preventDefault()
    if (this._isSelected(name)) {
      this.setState({selected: without(this.state.selected, name)})
    } else {
      this.setState({selected: concat(this.state.selected, name)})
    }
  }

  @autobind
  _isSelected(name) {
    return this.state.selected.indexOf(name) > -1
  }

  render() {
    //TODO
    /*const cols = this.props.artifacts.map((artifact, idx) => {
      const isSelected = this._isSelected(artifact.name)
      const kind = (artifact.value.kind.base == 'map') ? artifact.value.kind.args[1] : artifact.value.kind.args[0]
      const unit = (kind == 'distance') ? 'meters' : (kind == 'duration') ? 'milliseconds' : null

      let data = (artifact.value.kind.base == 'map') ? values(artifact.value.payload) : artifact.value.payload
      if (this.state.type == 'cdf') {
        data = _cdf(data, STEPS)
      } else if (this.state.type == 'pdf') {
        data = _pdf(data, STEPS)
      }

      return (<Col md={isSelected ? 12 : 4} key={idx}>
        <Plot name={artifact.name}
              data={data}
              unit={unit}
              type={this.state.type}
              width={(isSelected) ? 1110 : 350}
              height={(isSelected) ? 500 : 350}/>
        <a href="#" onClick={e => this._handleSelect(e, artifact.name)} className="accio-summary-resize">
          <Glyphicon glyph={(isSelected) ? 'resize-small' : 'resize-full'} />
        </a>
      </Col>);
    });
    return (
      <div>
        <div className="center">
          <ButtonGroup>
            <Button active={'pdf' === this.state.type} onClick={() => this._handleChange('pdf')}>
              PDF
            </Button>
            <Button active={'cdf' === this.state.type} onClick={() => this._handleChange('cdf')}>
              CDF
            </Button>
          </ButtonGroup>
        </div>
        <Row>{cols}</Row>
      </div>
    );*/
    return null;
  }
}

PlotArtifactGroup.propTypes = {
  artifacts: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
}

export default PlotArtifactGroup;
