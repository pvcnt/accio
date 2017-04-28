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

# When you update this version, please make sure you update other libraries that are included here, to
# avoid dependency hell. Known dependencies that must stay in sync:
#   - com/twitter/finagle
#   - com/twitter/util
#   - com/twitter/inject
#   - com/twitter/bijection
#   - com/fasterxml/jackson
VERSION = '2.10.0'

jar_library(name='http', jars=[scala_jar('com.twitter', 'finatra-http', VERSION)])
jar_library(name='httpclient', jars=[scala_jar('com.twitter', 'finatra-httpclient', VERSION)])
jar_library(name='thrift', jars=[scala_jar('com.twitter', 'finatra-thrift', VERSION)])
jar_library(name='jackson', jars=[scala_jar('com.twitter', 'finatra-jackson', VERSION)])
jar_library(name='slf4j', jars=[scala_jar('com.twitter', 'finatra-slf4j', VERSION)])

jar_library(name='scalap-compiler-deps', jars=[scala_jar('com.twitter.finatra', 'finatra-scalap-compiler-deps', '2.0.0')])
