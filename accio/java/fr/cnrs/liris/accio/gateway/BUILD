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

scala_binary(
  name = "gateway",
  srcs = glob(["*.scala"]),
  deps = [
    "//accio/java/fr/cnrs/liris/accio/api",
    "//accio/java/fr/cnrs/liris/accio/logging",
    "//accio/java/fr/cnrs/liris/common/util",
    "//accio/java/fr/cnrs/liris/common/geo",
    "//accio/thrift/fr/cnrs/liris/accio/agent",
    "//3rdparty/jvm/com/twitter:finatra_http",
    "//3rdparty/jvm/com/twitter:finatra_httpclient",
  ],
  resources = [
    "//accio/node/fr/cnrs/liris/accio/gateway:index.bundle.js",
    "//accio/node/fr/cnrs/liris/accio/gateway:index.html",
    "//accio/node/fr/cnrs/liris/accio/gateway:robots.txt",
    "//accio/node/fr/cnrs/liris/accio/gateway:favicon.ico",
  ],
  resource_strip_prefix = "accio/node/fr/cnrs/liris/accio/gateway",
  main_class = "fr.cnrs.liris.accio.gateway.GatewayServerMain",
)
