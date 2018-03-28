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
  name = "service",
  srcs = glob(["*.scala"]),
  deps = [
    "//accio/java/fr/cnrs/liris/accio/service",
    "//accio/java/fr/cnrs/liris/accio/api",
    "//accio/thrift/fr/cnrs/liris/accio/api",
    "//accio/java/fr/cnrs/liris/accio/sdk",
    "//accio/java/fr/cnrs/liris/accio/sdk:annotations",
    "//accio/java/fr/cnrs/liris/accio/discovery",
    "//accio/java/fr/cnrs/liris/accio/discovery/reflect",
    "//accio/java/fr/cnrs/liris/common/util",
    "//accio/javatests/fr/cnrs/liris/testing",
    "//accio/javatests/fr/cnrs/liris/accio/testing",
    "//3rdparty/jvm/com/google/guava",
    "//3rdparty/jvm/com/twitter:util_slf4j_api",
    "//3rdparty/jvm/com/github/nscala_time",
  ],
)
