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
  sources = globs('*.scala'),
  dependencies = [
    ':resources',
    'src/jvm/fr/cnrs/liris/accio/tools/cli/config',
    'src/jvm/fr/cnrs/liris/accio/tools/cli/command',
    'src/jvm/fr/cnrs/liris/accio/logging',
    'src/jvm/fr/cnrs/liris/common/io',
    '3rdparty/jvm/com/twitter/inject:core',
    '3rdparty/jvm/com/twitter/inject:slf4j',
    '3rdparty/jvm:logback',
    '3rdparty/jvm/org/slf4j:jul-to-slf4j',
  ],
  excludes = [
    # Multiple logging frameworks.
    exclude('commons-logging', 'commons-logging'),
    # Finagle comes with its own Thrift library.
    exclude(org = 'com.twitter', name = 'libthrift'),
  ],
)

resources(
  name = 'resources',
  sources = ['logback.xml'],
)

jvm_binary(
  name = 'bin',
  dependencies = [':cli'],
  main = 'fr.cnrs.liris.accio.tools.cli.AccioClientMain',
  basename = 'accio-client',
)
