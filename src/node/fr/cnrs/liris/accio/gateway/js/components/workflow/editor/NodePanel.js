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

import React from 'react';
import {find, fromPairs, noop, some, has, findIndex, forEach, cloneDeep} from 'lodash';
import {
  Panel,
  ListGroup,
  ListGroupItem,
  FormGroup,
  ControlLabel,
  FormControl,
  HelpBlock,
  Button,
  ButtonToolbar
} from 'react-bootstrap';
import autobind from 'autobind-decorator';
import {prettyPrintKind} from' ../../../utils/prettyPrint';

const initState = (nodeDef, opDef) => {
  const inputs = fromPairs(opDef.inputs.map((inputDef) => {
    if (nodeDef.inputs[inputDef.name]) {
      return [inputDef.name, nodeDef.inputs[inputDef.name]];
    } else if (inputDef.default_value) {
      return [inputDef.name, { value: inputDef.default_value }];
    } else {
      return [inputDef.name, null];
    }
  }));
  const validationState = fromPairs(opDef.inputs.map((inputDef) => {
    if (inputDef.is_optional && !inputs[inputDef.name]) {
      return [];
    } else {
      return [inputDef.name, {status: 'success'}];
    }
  }));
  return { name: nodeDef.name, inputs, validationState };
};

class NodePanel extends React.Component {
  constructor(props) {
    super(props);
    this.state = { name: null, inputs: {}, validationState: {} };
  }

  componentWillReceiveProps(nextProps) {
    const opDef = find(nextProps.operators, op => op.name === nextProps.node.op);
    this.setState(initState(nextProps.node, opDef));
  }

  componentDidMount() {
    const opDef = find(this.props.operators, op => op.name === this.props.node.op);
    this.setState(initState(this.props.node, opDef));
  }

  @autobind
  handleSubmit(e) {
    e.nativeEvent.preventDefault();
    const workflow = cloneDeep(this.props.workflow);
    const idx = findIndex(workflow.graph, node => node.name === this.props.node.name);
    workflow.graph[idx].inputs = this.state.inputs;
    if (this.state.name !== this.props.node.name) {
      workflow.graph[idx].name = this.state.name;
      workflow.graph.forEach((node) => {
        forEach(node.inputs, (v, k) => {
          if (v.reference && v.reference.node === this.props.node.name) {
            node.inputs[k].reference.node = this.state.name;
          }
        });
      });
    }
    this.props.onChange(workflow);
  }

  @autobind
  handleDelete() {
    const workflow = cloneDeep(this.props.workflow);
    const idx = findIndex(workflow.graph, node => node.name === this.props.node.name);
    delete workflow.graph[idx];
    this.props.onChange(workflow);
  }

  @autobind
  handleChangeName(e) {
    this.setState({ name: e.target.value });
  }

  @autobind
  handleChange(name, e) {
    let inputs = {};
    let validationState = {};
    const opDef = find(this.props.operators, op => op.name === this.props.node.op);
    const inputDef = find(opDef.inputs, inDef => inDef.name === name);

    if (e.target.value) {
      let value = e.target.value;
      let status = 'success';
      let help = null;
      if (value[0] === '@') {
        value = value.substring(1);
        if (value) {
          const pos = value.indexOf('/');
          if (pos === -1) {
            const targetParam = find(this.props.workflow.params, param => param.name === value);
            if (!targetParam) {
              help = 'Unknown parameter name';
              status = 'error';
            } else if (targetParam.kind.base !== inputDef.kind.base) {
              help = 'Invalid parameter type: ' + prettyPrintKind(inputDef.kind);
              status = 'error';
            }
            value = {param: value};
          } else {
            const reference = {node: value.substring(0, pos), port: value.substring(pos + 1)};
            const targetNode = find(this.props.workflow.graph, node => node.name === reference.node);
            if (!targetNode) {
              help = 'Unknown node name';
              status = 'error';
            } else {
              const targetOpDef = find(this.props.operators, op => op.name === targetNode.name);
              if (!some(targetOpDef.inputs, inDef => inDef.name === reference.port)) {
                help = 'Unknown port name';
                status = 'error';
              }
            }
            value = {reference};
          }
        } else {
          // Typing a reference, only "@" has been typed so far.
          value = { param: value };
          status = 'error';
        }
      } else {
        value = { value: {kind: inputDef.kind, payload: value} };
      }
      Object.assign(inputs, this.state.inputs);
      inputs[name] = value;
      Object.assign(validationState, this.state.validationState);
      validationState[name] = { status, help };
    } else {
      Object.assign(inputs, this.state.inputs);
      delete inputs[name];
      Object.assign(validationState, this.state.validationState);
      if (inputDef.is_optional) {
        delete validationState[name];
      } else {
        validationState[name] = { status: 'error', help: 'Required' };
      }
    }
    this.setState({ inputs, validationState });
  }

  render() {
    const {node} = this.props;
    const opDef = find(this.props.operators, op => op.name === node.op);
    const inputs = opDef.inputs.map((inputDef) => {
      let control = null;
      const value = this.state.inputs[inputDef.name];
      if (value) {
        if (has(value, 'param')) {
          control = <FormControl type="text"
                                 value={'@' + value.param}
                                 onChange={e => this.handleChange(inputDef.name, e)}/>;
        } else if (value.reference) {
          control = <FormControl type="text"
                                 value={'@' + value.reference.node + '/' + value.reference.port}
                                 onChange={e => this.handleChange(inputDef.name, e)}/>;
        } else if (value.value) {
          control = <FormControl type="text"
                                 value={value.value.payload}
                                 onChange={e => this.handleChange(inputDef.name, e)}/>;
        } else {
          console.log('ERROR: Invalid value for input ' + inputDef.name);
          console.log(value);
        }
      } else {
        control = <FormControl type="text"
                               value=""
                               onChange={e => this.handleChange(inputDef.name, e)}/>;
      }
      const status = this.state.validationState[inputDef.name] ? this.state.validationState[inputDef.name].status : null;
      let help;
      if (this.state.validationState[inputDef.name]) {
        help = this.state.validationState[inputDef.name].help;
      } else if (inputDef.help) {
        help = inputDef.help;
      }
      return (
        <FormGroup validationState={status} key={inputDef.name}>
          <ControlLabel>{inputDef.name + ' (' + prettyPrintKind(inputDef.kind) + ')'}</ControlLabel>
          {control}
          <FormControl.Feedback />
          {help ? <HelpBlock>{help}</HelpBlock> : null}
        </FormGroup>
      );
    });
    return (
      <form onSubmit={this.handleSubmit}>
        <Panel collapsible defaultExpanded header={node.name}>
          {opDef.help ? <span className="operator-help">{opDef.help}</span> : null}
          <ListGroup fill>
            <ListGroupItem>
              <FormGroup validationState={this.state.name ? 'success' : 'error'}>
                <ControlLabel>Node name</ControlLabel>
                <FormControl type="text"
                             value={this.state.name ? this.state.name : ''}
                             onChange={this.handleChangeName}/>
                <FormControl.Feedback />
              </FormGroup>
            </ListGroupItem>
            <ListGroupItem>
              {inputs}
              <ButtonToolbar>
                <Button type="submit" bsStyle="primary">Save</Button>
                <Button onClick={this.handleDelete}>Delete</Button>
                <Button onClick={this.props.onClose}>Cancel</Button>
              </ButtonToolbar>
            </ListGroupItem>
          </ListGroup>
        </Panel>
      </form>
    );
  }
}

NodePanel.propTypes = {
  workflow: React.PropTypes.object.isRequired,
  node: React.PropTypes.object.isRequired,
  operators: React.PropTypes.array.isRequired,
  onChange: React.PropTypes.func.isRequired,
  onClose: React.PropTypes.func.isRequired,
};
NodePanel.defaultProps = {
  onClose: noop,
  onChange: noop,
};

export default NodePanel;
