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
import {Panel, ListGroup, ListGroupItem} from 'react-bootstrap'

class NodePanel extends React.Component {
  render() {
    const { node } = this.props;
    return (
      <Panel collapsible defaultExpanded header={node.name}>
        Some default panel content here.
        <ListGroup fill>
          <ListGroupItem>Item 1</ListGroupItem>
          <ListGroupItem>Item 2</ListGroupItem>
          <ListGroupItem>&hellip;</ListGroupItem>
        </ListGroup>
        Some more panel content here.
      </Panel>
    );
  }
}

NodePanel.propTypes = {
  node: React.PropTypes.object.isRequired,
};

export default NodePanel;
