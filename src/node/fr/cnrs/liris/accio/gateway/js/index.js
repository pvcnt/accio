const React = require('react');
const ReactDOM = require('react-dom');
import "babel-polyfill";
import "whatwg-fetch";
import App from "./App";

ReactDOM.render(<App />, document.getElementById('app'));