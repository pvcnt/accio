import { createStore, applyMiddleware } from 'redux';
import rootReducer from './reducers';
import thunkMiddleware from 'redux-thunk';
import loggerMiddleware from 'redux-logger';

export default createStore(rootReducer, applyMiddleware(loggerMiddleware, thunkMiddleware));