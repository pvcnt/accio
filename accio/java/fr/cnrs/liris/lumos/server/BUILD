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
  name = "server",
  srcs = glob(["*.scala"]),
  deps = [
    "//accio/java/fr/cnrs/liris/lumos/domain",
    "//accio/java/fr/cnrs/liris/lumos/domain/thrift",
    "//accio/java/fr/cnrs/liris/lumos/state",
    "//accio/java/fr/cnrs/liris/lumos/storage",
    "//accio/java/fr/cnrs/liris/lumos/storage/install",
    "//accio/java/fr/cnrs/liris/lumos/version",
    "//accio/java/fr/cnrs/liris/infra/thriftserver",
    "//accio/thrift/fr/cnrs/liris/lumos/server",
    "//3rdparty/jvm/com/twitter:finatra_thrift",
    "//3rdparty/jvm/com/twitter:util_slf4j_api",
    "//3rdparty/jvm/com/google/guava",
  ],
  runtime_deps = [
    "//3rdparty/jvm/ch/qos/logback:logback_classic",
    "//3rdparty/jvm/com/twitter:twitter_server_logback_classic",
  ],
  resource_strip_prefix = "accio/java",
  main_class = "fr.cnrs.liris.lumos.server.LumosServerMain",
)
