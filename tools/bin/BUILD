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

load("@build_bazel_rules_nodejs//:defs.bzl", "nodejs_binary")

nodejs_binary(
  name = "webpack",
  node_modules = "//3rdparty:node_modules",
  entry_point = "webpack/bin/webpack.js",
  visibility = ["//visibility:public"],
)

nodejs_binary(
  name = "eslint",
  node_modules = "//3rdparty:node_modules",
  entry_point = "eslint/bin/eslint.js",
  visibility = ["//visibility:public"],
)
