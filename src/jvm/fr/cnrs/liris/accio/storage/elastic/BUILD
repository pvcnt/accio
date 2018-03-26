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
  name = 'elastic',
  sources = globs('*.scala'),
  dependencies = [
    ':annotations',
    'src/jvm/fr/cnrs/liris/accio/storage',
    'src/jvm/fr/cnrs/liris/accio/util',
    'src/thrift/fr/cnrs/liris/accio/api',
    '3rdparty/jvm/com/sksamuel/elastic4s:core',
    '3rdparty/jvm/com/sksamuel/elastic4s:jackson',
    '3rdparty/jvm/com/typesafe/scala-logging',
    '3rdparty/jvm:scrooge-core',
    '3rdparty/jvm/com/twitter/inject:core',
    '3rdparty/jvm/com/twitter/finatra:jackson',
    '3rdparty/jvm/com/twitter/bijection:util',
  ],
)

java_library(
  name = 'annotations',
  sources = globs('*.scala'),
  dependencies = [
    '3rdparty/jvm/com/twitter/inject:core',
  ],
)