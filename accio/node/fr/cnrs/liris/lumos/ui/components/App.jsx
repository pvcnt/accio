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

import React from 'react';
import { Spinner } from '@blueprintjs/core';
import { Route, Redirect } from 'react-router-dom';
import autobind from 'autobind-decorator';
import Navbar from './Navbar';

import JobsView from './jobs/JobsView';
import JobViewContainer from './jobs/JobViewContainer';
import LoginDialog from './LoginDialog';

export default class App extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      loading: false,
      authenticated: true,
    };
  }

  componentDidMount() {
    // checkAuthenticated().then(authenticated => this.setState({ authenticated, loading: false }));
  }

  @autobind
  handleLogin() {
    this.setState({ authenticated: true });
  }

  @autobind
  handleLogout() {
    this.setState({ authenticated: false });
  }

  render() {
    let content;
    if (this.state.loading) {
      content = <Spinner/>;
    } else if (!this.state.authenticated) {
      content = <LoginDialog onLogin={this.handleLogin}/>;
    } else {
      content = [
        <Route exact path="/" render={() => <Redirect to="/jobs"/>}/>,
        <Route exact path="/jobs" component={JobsView}/>,
        <Route exact path="/jobs/view/:name" component={JobViewContainer}/>,
      ];
      content = React.Children.map(content, (route, idx) => React.cloneElement(route, { key: idx }));
    }
    return (
      <div className="page">
        <Navbar onLogout={this.handleLogout}/>
        <div className="container content">
          {content}
        </div>
      </div>
    );
  }
};