const React = require('react');
import {Grid, Row, Col} from "react-bootstrap";
import {Link} from "react-router";

/**
 * Component serving Accio UI's home page.
 */
let HomePage = React.createClass({
  render: function () {
    return (
      <div>
        <div className="home-banner">
          <h1>Power your experiments</h1>
          <p>Accio is a scientific experimentation platform.</p>
        </div>
        <Grid>
          <Row>
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
              <Link to="/runs">
                <img src="/images/bars-128px.png"/>
                <h3>Analyse runs</h3>
                <p>
                  Monitor the execution of runs and visualise their results.
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
    );
  }
});

export default HomePage;
