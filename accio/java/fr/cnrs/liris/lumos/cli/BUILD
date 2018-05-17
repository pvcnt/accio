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
    "//accio/java/fr/cnrs/liris/lumos/domain",
    "//accio/java/fr/cnrs/liris/lumos/domain/thrift",
    "//accio/java/fr/cnrs/liris/lumos/version",
    "//accio/java/fr/cnrs/liris/infra/cli",
    "//accio/java/fr/cnrs/liris/util",
    "//accio/thrift/fr/cnrs/liris/lumos/server",
  ],
  main_class = "fr.cnrs.liris.lumos.cli.LumosClientMain",
)

genrule(
  name = "binary",
  srcs = ["cli_deploy.jar"],
  outs = ["cli_binary"],
  cmd = "cat $(location stub.sh) $(location cli_deploy.jar)> $@",
  tools = ["stub.sh"],
)