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
  name = "thriftserver",
  srcs = glob(["*.scala"]),
  size = "small",
  deps = [
    "//accio/java/fr/cnrs/liris/infra/thriftserver",
    "//accio/java/fr/cnrs/liris/infra/webhook",
    "//accio/javatests/fr/cnrs/liris/testing",
    "//accio/javatests/com/twitter/inject",
    "//3rdparty/jvm/com/twitter:util_core",
    "//3rdparty/jvm/com/twitter:inject_core",
    "//3rdparty/jvm/com/twitter:finagle_http",
    "//3rdparty/jvm/com/twitter:util_slf4j_api",
  ],
  resources = ["static_file.txt"],
  resource_strip_prefix = "accio/javatests",
)