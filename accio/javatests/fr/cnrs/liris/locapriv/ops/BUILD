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

scala_test(
  name = "ops",
  srcs = glob(["*.scala"]),
  deps = [
    "//accio/java/fr/cnrs/liris/locapriv/ops",
    "//accio/java/fr/cnrs/liris/locapriv/domain",
    "//accio/java/fr/cnrs/liris/sparkle",
    "//accio/java/fr/cnrs/liris/sparkle/format",
    "//accio/java/fr/cnrs/liris/accio/sdk",
    "//accio/java/fr/cnrs/liris/util/geo",
    "//accio/javatests/fr/cnrs/liris/locapriv/testing",
    "//accio/javatests/fr/cnrs/liris/testing",
    "//3rdparty/jvm/com/github/nscala_time",
    "//3rdparty/jvm/org/scalanlp:breeze",
    "//3rdparty/jvm/com/twitter:util_slf4j_api",
    "//3rdparty/jvm/com/twitter:util_jvm",
  ],
  size = "small",
)
