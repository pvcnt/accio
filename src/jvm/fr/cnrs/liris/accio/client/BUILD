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

scala_library(
  dependencies=[
    'src/jvm/fr/cnrs/liris/accio/core/infra/cli',
    'src/jvm/fr/cnrs/liris/accio/client/command',
    'src/jvm/fr/cnrs/liris/accio/client/service',

    '3rdparty/jvm/com/typesafe/scala-logging',
    '3rdparty/jvm:scala-guice',
    '3rdparty/jvm:logback',
    '3rdparty/jvm/org/slf4j:jul-to-slf4j',
  ],
  excludes = [
    # common/reflect comes with its own Scala compiler library, we do not need the one provided by Finatra.
    exclude(org='com.twitter.finatra', name='finatra-scalap-compiler-deps_2.11'),
    # Finagle comes with its own Thrift library.
    exclude(org = 'com.twitter', name = 'libthrift'),
  ],
  sources=globs('*.scala')
)

jvm_binary(
  name='bin',
  dependencies=[
    ':client',
  ],
  resources=[
    'src/resources/fr/cnrs/liris/accio/client',
  ],
  main='fr.cnrs.liris.accio.client.AccioClientMain',
  basename='accio-client',
)
