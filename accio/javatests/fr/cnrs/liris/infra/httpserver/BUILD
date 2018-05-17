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

load("@io_bazel_rules_scala//thrift:thrift.bzl", "thrift_library")
load("@io_bazel_rules_scala//twitter_scrooge:twitter_scrooge.bzl", "scrooge_scala_library")

scala_test(
  name = "httpserver",
  srcs = glob(["*.scala"]),
  size = "small",
  deps = [
    ":scrooge",
    "//accio/java/fr/cnrs/liris/infra/httpserver",
    "//accio/javatests/fr/cnrs/liris/testing",
    "//3rdparty/jvm/com/twitter:finatra_jackson",
  ],
)

scrooge_scala_library(
  name = "scrooge",
  deps = [":thrift"],
)

thrift_library(
  name = "thrift",
  srcs = glob(["*.thrift"]),
)
