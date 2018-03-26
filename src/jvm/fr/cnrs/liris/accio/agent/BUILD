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
  name = 'agent',
  sources = globs('*.scala'),
  dependencies = [
    ':resources',
    'src/jvm/fr/cnrs/liris/accio/storage/inject',
    'src/jvm/fr/cnrs/liris/accio/scheduler/inject',
    'src/jvm/fr/cnrs/liris/accio/discovery/inject',
    'src/jvm/fr/cnrs/liris/accio/config',
    'src/jvm/fr/cnrs/liris/accio/config:annotations',
    'src/jvm/fr/cnrs/liris/accio/api',
    'src/jvm/fr/cnrs/liris/accio/storage',
    'src/jvm/fr/cnrs/liris/accio/dsl',
    'src/jvm/fr/cnrs/liris/accio/service',
    'src/jvm/fr/cnrs/liris/accio/scheduler',
    'src/jvm/fr/cnrs/liris/accio/util',
    'src/jvm/fr/cnrs/liris/accio/logging',
    'src/thrift/fr/cnrs/liris/accio/agent',
    'src/thrift/fr/cnrs/liris/accio/api',
    '3rdparty/jvm/com/twitter/inject:core',
    '3rdparty/jvm/com/twitter/inject:slf4j',
    '3rdparty/jvm/com/twitter/inject:server',
    '3rdparty/jvm/com/twitter/inject:modules',
    '3rdparty/jvm/com/twitter/finagle:thrift',
    '3rdparty/jvm/com/google/guava',
    scoped('src/jvm/fr/cnrs/liris/locapriv/ops', 'runtime'),
  ],
  excludes = [
    # Multiple logging frameworks.
    exclude('org.slf4j', 'slf4j-log4j12'),
    exclude('org.slf4j', 'slf4j-jdk14'),
    exclude('log4j', 'log4j'),
    exclude('commons-logging', 'commons-logging'),
    # We already include canonical Thrift library, we do not need the one provided by Finagle.
    exclude('com.twitter', 'libthrift'),
    # common/reflect comes with its own Scala compiler library, we do not need the one provided by Finatra.
    #exclude('com.twitter.finatra', 'finatra-scalap-compiler-deps_2.11'),
  ],
)

resources(
  name='resources',
  sources=['logback.xml']
)

jvm_binary(
  name='bin',
  dependencies=[':agent'],
  main='fr.cnrs.liris.accio.agent.AgentServerMain',
  basename='accio-agent',
)
