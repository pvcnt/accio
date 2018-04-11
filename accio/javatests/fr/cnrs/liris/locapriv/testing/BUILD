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
  name = "testing",
  deps=[
    "//accio/java/fr/cnrs/liris/locapriv/model",
    "//accio/java/fr/cnrs/liris/locapriv/io",
    "//accio/java/fr/cnrs/liris/util/geo",
    "//3rdparty/jvm/com/github/nscala_time",
    "//3rdparty/jvm/com/google/guava",
  ],
  srcs = glob(["*.scala"]),
  resources = ["abboip.csv"],
  resource_strip_prefix = "accio/javatests",
  visibility = ["//accio/javatests:__subpackages__"],
)
