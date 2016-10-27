const React = require('react');
import {Router, Route, IndexRoute, hashHistory} from "react-router";
import AppController from "./components/AppController";
import Home from "./components/home/Home";
import RunSection from "./components/run/RunSection";
import RunListContainer from "./components/run/RunListContainer";
import RunViewContainer from "./components/run/RunViewContainer";
import CreateRunContainer from "./components/run/CreateRunContainer";
import WorkflowSection from "./components/workflow/WorkflowSection";
import WorkflowListContainer from "./components/workflow/WorkflowListContainer";
import WorkflowViewContainer from "./components/workflow/WorkflowViewContainer";

const App = React.createClass({
  render: function () {
    return (
      <Router history={hashHistory}>
        <Route path="/" component={AppController}>
          <IndexRoute component={Home}/>
          <Route path="runs" component={RunSection}>
            <IndexRoute component={RunListContainer}/>
            <Route path="view/:id" component={RunViewContainer}/>
            <Route path="create/:workflow" component={CreateRunContainer}/>
          </Route>
          <Route path="workflows" component={WorkflowSection}>
            <IndexRoute component={WorkflowListContainer}/>
            <Route path="view/:id" component={WorkflowViewContainer}/>
            <Route path="view/:id/:version" component={WorkflowViewContainer}/>
          </Route>
        </Route>
      </Router>
    );
  }
});

export default App;
