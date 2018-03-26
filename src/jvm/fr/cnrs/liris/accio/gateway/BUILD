# Accio is a platform to launch computer science experiments.
# Copyright (C) 2016-2018 Vincent Primault <v.primault@ucl.ac.uk>
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

scala_library(
  name = 'gateway',
  sources = globs('*.scala'),
  dependencies = [
    ':resources',
    'src/jvm/fr/cnrs/liris/accio/api',
    'src/jvm/fr/cnrs/liris/common/util',
    'src/thrift/fr/cnrs/liris/accio/agent',
    'src/node/fr/cnrs/liris/accio/gateway',
    '3rdparty/jvm/com/twitter/finatra:http',
    '3rdparty/jvm/com/twitter/finatra:httpclient',
    '3rdparty/jvm:logback',
  ],
  excludes = [
    # Multiple logging frameworks.
    exclude('org.slf4j', 'slf4j-log4j12'),
    exclude('org.slf4j', 'slf4j-jdk14'),
    exclude('log4j', 'log4j'),
    exclude('commons-logging', 'commons-logging'),
    # We already include canonical Thrift library, we do not need the one provided by Finagle.
    exclude('com.twitter', 'libthrift'),
  ],
)

resources(
  name = 'resources',
  sources = ['logback.xml'],
)

jvm_binary(
  name = 'bin',
  dependencies = [':gateway'],
  main = 'fr.cnrs.liris.accio.gateway.GatewayServerMain',
  basename = 'accio-gateway',
)
