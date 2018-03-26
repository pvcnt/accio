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
  dependencies=[
    ':resources',
    'src/jvm/fr/cnrs/liris/accio/sdk',
    'src/jvm/fr/cnrs/liris/accio/sdk:annotations',
    'src/jvm/fr/cnrs/liris/locapriv/sparkle',
    'src/jvm/fr/cnrs/liris/locapriv/model',
    'src/jvm/fr/cnrs/liris/locapriv/lppm',
    'src/jvm/fr/cnrs/liris/locapriv/clustering',
    'src/jvm/fr/cnrs/liris/locapriv/io',
    'src/jvm/fr/cnrs/liris/common/geo',
    'src/jvm/fr/cnrs/liris/common/util',
    'src/jvm/fr/cnrs/liris/common/random',
    'src/jvm/com/google/common/geometry',
    '3rdparty/jvm:scala-time',
    '3rdparty/jvm/org/scalanlp:breeze',
    '3rdparty/jvm/com/twitter/util:jvm',
  ],
  sources=globs('*.scala'),
)

resources(
  name='resources',
  sources=['linux_w4m_LST', 'linux_w4m_LST_chunk', 'mac_w4m_LST', 'mac_w4m_LST_chunk']
)
