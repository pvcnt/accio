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

package fr.cnrs.liris.accio.domain

import fr.cnrs.liris.lumos.domain.AttrValue

/**
 * Payload containing the information needed to execute an operator. It is given to the
 * operator's binary as a based-64 encoded command-line argument.
 *
 * @param op        Name of the operator to execute.
 * @param seed      Seed used by unstable operators (included even if the operator is not unstable).
 * @param params    Input values of the operator. All values should be included, even the optional ones.
 * @param resources Compute resources necessary to execute the operator. This may be used by some
 *                  implementations to provide some isolation, if a proper isolator is not available.
 */
case class OpPayload(op: String, seed: Long, params: Seq[AttrValue], resources: Map[String, Long])