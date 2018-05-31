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
  name = "cli",
  srcs = glob(["*.scala"]),
  deps = [
    "//accio/java/fr/cnrs/liris/accio/domain",
    "//accio/java/fr/cnrs/liris/accio/domain/thrift",
    "//accio/java/fr/cnrs/liris/accio/dsl",
    "//accio/java/fr/cnrs/liris/accio/dsl/json",
    "//accio/java/fr/cnrs/liris/accio/version",
    "//accio/java/fr/cnrs/liris/lumos/domain",
    "//accio/thrift/fr/cnrs/liris/accio/server",
    "//accio/java/fr/cnrs/liris/infra/cli/app",
    "//accio/java/fr/cnrs/liris/infra/cli/io",
    "//accio/java/fr/cnrs/liris/util",
    "//3rdparty/jvm/ch/qos/logback:logback_core",
    "//3rdparty/jvm/ch/qos/logback:logback_classic",
    "//3rdparty/jvm/com/twitter:util_slf4j_jul_bridge",
  ],
  main_class = "fr.cnrs.liris.accio.cli.AccioClientMain",
)

genrule(
  name = "binary",
  srcs = ["cli_deploy.jar"],
  outs = ["cli_binary"],
  cmd = "cat $(location stub.sh) $(location cli_deploy.jar)> $@",
  tools = ["stub.sh"],
)