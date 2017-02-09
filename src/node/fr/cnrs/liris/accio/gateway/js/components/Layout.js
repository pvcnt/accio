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
import {Link} from 'react-router'
import {Navbar, Nav, NavItem, Glyphicon, Grid} from 'react-bootstrap'
import {LinkContainer} from 'react-router-bootstrap'

class Layout extends React.Component {
  render() {
    return (
      <div>
        <Navbar fixedTop>
          <Navbar.Header>
            <Navbar.Brand>
              <Link to="/">Accio</Link>
            </Navbar.Brand>
          </Navbar.Header>
          <Nav>
            <LinkContainer to="runs">
              <NavItem>Runs</NavItem>
            </LinkContainer>
            <LinkContainer to="workflows">
              <NavItem>Workflows</NavItem>
            </LinkContainer>
          </Nav>
          <Nav pullRight>
            <NavItem href="https://privamov.github.io/accio/docs">Help</NavItem>
            <Navbar.Text>
              <Glyphicon glyph="user"/> vprimault
            </Navbar.Text>
          </Nav>
        </Navbar>

        {this.props.children}

        <Grid>
          <footer>
            Made with <Glyphicon glyph="heart"/> at
            <a href="http://liris.cnrs.fr"><img src="images/liris.png" alt="LIRIS"/></a>
          </footer>
        </Grid>
      </div>
    )
  }
}

export default Layout
