import React from "react";
import {Link} from "react-router";
import {Navbar, Nav, NavItem, Grid, Glyphicon, MenuItem, Row} from "react-bootstrap";
import {LinkContainer} from "react-router-bootstrap";

var AppController = React.createClass({
  render: function () {
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
            <NavItem href="https://pvcnt.github.io/location-privacy/">Help</NavItem>
            <Navbar.Text>
              <Glyphicon glyph="user"/> vincent
            </Navbar.Text>
          </Nav>
        </Navbar>

        {this.props.children}

        <Row>
          <footer>
            <span className="glyphicon glyphicon-copyright-mark"/> 2016
            <a href="http://liris.cnrs.fr"><img src="images/liris.png" alt="LIRIS"/></a>
          </footer>
        </Row>
      </div>
    );
  }
});

export default AppController;
