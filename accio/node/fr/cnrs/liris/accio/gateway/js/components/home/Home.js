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
import {Grid, Row, Col} from 'react-bootstrap'
import {Link} from 'react-router'

/**
 * Component displaying the homepage.
 */
class HomePage extends React.Component {
  render() {
    return <div>
      <div className="home-banner">
        <h1>Power your experiments</h1>
        <p>Accio is your scientific workflow management platform.</p>
      </div>
      <Grid>
        <Row>
          <Col sm={3} className="home-box">
            <Link to="/runs">
              <img src="/images/bars-128px.png"/>
              <h3>Analyze runs</h3>
              <p>
                Monitor the execution of runs and visualize their results.
              </p>
            </Link>
          </Col>
          <Col sm={3} className="home-box">
            <Link to="/workflows">
              <img src="/images/bookshelf-128px.png"/>
              <h3>Explore workflows</h3>
              <p>
                Explore the library of available workflows and view their details.
              </p>
            </Link>
          </Col>
          <Col sm={3} className="home-box">
            <Link to="/runs/create">
              <img src="/images/keyboard-128px.png"/>
              <h3>Launch a new run</h3>
              <p>Pick an existing workflow and create an experiment based on it.</p>
            </Link>
          </Col>
          <Col sm={3} className="home-box">
            <Link to="/config">
              <img src="/images/gear-128px.png"/>
              <h3>Configuration</h3>
              <p>View Accio status.</p>
            </Link>
          </Col>
        </Row>
      </Grid>
    </div>
  }
}

export default HomePage
