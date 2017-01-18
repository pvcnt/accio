# Accio is a program whose purpose is to study location privacy.
# Copyright (C) 2016-2017 Vincent Primault <vincent.primault@liris.cnrs.fr>
#
# Accio is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# Accio is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with Accio.  If not, see <http://www.gnu.org/licenses/>.

VERSION = '5.1.5'

jar_library(name='core', jars=[scala_jar('com.sksamuel.elastic4s', 'elastic4s-core', VERSION)])
jar_library(name='jackson', jars=[scala_jar('com.sksamuel.elastic4s', 'elastic4s-jackson', VERSION)])
jar_library(
  name='testkit',
  jars=[
    scala_jar('com.sksamuel.elastic4s', 'elastic4s-testkit', VERSION),
    # Painless plugin is not included by default, and not available in official org.elasticsearch Maven repository
    # (why??), as of 01/17/2017.
    jar('org.codelibs.elasticsearch.module', 'lang-painless', '5.1.2'),
  ]
)