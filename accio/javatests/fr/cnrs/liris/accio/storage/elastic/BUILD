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

#scala_test(
#  name = "elastic",
#  srcs = glob(["*.scala"]),
#  deps = [
#    "//accio/java/fr/cnrs/liris/accio/storage/elastic",
#    "//accio/java/fr/cnrs/liris/accio/api",
#    "//accio/java/fr/cnrs/liris/accio/util",
#    "//accio/javatests/fr/cnrs/liris/accio/storage",
#    "//accio/javatests/fr/cnrs/liris/testing",
#    "//3rdparty/jvm/com/google/guava",
#    "//3rdparty/jvm/com/twitter:inject_core",
#    "//3rdparty/jvm/com/sksamuel/elastic4s:elastic4s_testkit",
    # Painless plugin is not included by default, and not available in official
    # org.elasticsearch Maven repository (why??), as of 01/17/2017.
#    "//3rdparty/jvm/org/codelibs/elasticsearch/module:lang_painless",
#  ],
#)

scrooge_scala_library(
  name = 'thrift',
  deps = [
    ':thriftlib',
  ],
)

thrift_library(
  name = 'thriftlib',
  srcs = glob(['*.thrift']),
)
