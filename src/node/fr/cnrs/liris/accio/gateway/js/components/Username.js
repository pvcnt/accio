/*
 * Accio is a program whose purpose is to study location privacy.
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

import React from 'react'

class Username extends React.Component {
  render() {
    return <span>
      {this.props.user.name}
      {this.props.user.email
        ? <span> &lt;<a href={'mailto:' + this.props.user.email}>{this.props.user.email}</a>&gt;</span>
        : null}
    </span>
  }
}

Username.propTypes = {
  user: React.PropTypes.object.isRequired
}

export default Username
