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
import React from "react";
import {noop} from "lodash";
import NodePanel from "./NodePanel";

class Sidebar extends React.Component {
  render() {
    return (
      <div className="workflow-editor-sidebar">
        {this.props.node
          ? <NodePanel workflow={this.props.workflow}
                       node={this.props.node}
                       operators={this.props.operators}
                       onChange={this.props.onChange}/>
          : null}
      </div>
    );
  }
}

Sidebar.propTypes = {
  workflow: React.PropTypes.object.isRequired,
  operators: React.PropTypes.array.isRequired,
  node: React.PropTypes.object,
  onChange: React.PropTypes.func.isRequired,
};
Sidebar.defaultProps = {
  onChange: noop,
};

export default Sidebar;
