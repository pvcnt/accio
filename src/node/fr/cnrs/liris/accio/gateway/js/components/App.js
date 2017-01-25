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
import {Router, Route, IndexRoute, hashHistory} from 'react-router'
import Layout from './Layout'
import Home from './home/Home'
import RunSection from './run/RunSection'
import RunListContainer from './run/list/RunListContainer'
import RunViewContainer from './run/view/RunViewContainer'
import WorkflowSection from './workflow/WorkflowSection'
import WorkflowListContainer from './workflow/list/WorkflowListContainer'
import WorkflowViewContainer from './workflow/view/WorkflowViewContainer'

class App extends React.Component {
  render() {
    return <Router history={hashHistory}>
      <Route path="/" component={Layout}>
        <IndexRoute component={Home}/>
        <Route path="runs" component={RunSection}>
          <IndexRoute component={RunListContainer}/>
          <Route path="view/:id" component={RunViewContainer}/>
        </Route>
        <Route path="workflows" component={WorkflowSection}>
          <IndexRoute component={WorkflowListContainer}/>
          <Route path="view/:id" component={WorkflowViewContainer}/>
          <Route path="view/:id/:version" component={WorkflowViewContainer}/>
        </Route>
      </Route>
    </Router>
  }
}

export default App
