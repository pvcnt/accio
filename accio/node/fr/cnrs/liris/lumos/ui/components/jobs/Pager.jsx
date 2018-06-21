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
import PropTypes from 'prop-types';
import { range } from 'lodash';
import classnames from 'classnames';
import { JOBS_PER_PAGE } from '../../constants';

class Pager extends React.Component {
  handleClick(e, page) {
    e.nativeEvent.preventDefault();
    if (page !== this.props.page) {
      this.props.onChange(page);
    }
  }

  render() {
    const pageCount = Math.ceil(this.props.totalCount / JOBS_PER_PAGE);
    const pages = range(1, pageCount + 1).map(n => {
      const className = classnames({ active: n === this.props.page });
      return <li key={n} className={className}><a onClick={e => this.handleClick(e, n)}>{n}</a></li>;
    });
    return <ul className="pager">{pages}</ul>;
  }
}

Pager.propTypes = {
  totalCount: PropTypes.number.isRequired,
  page: PropTypes.number.isRequired,
  onChange: PropTypes.func.isRequired,
};

export default Pager;