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

package(default_testonly = 1)

load("//accio/javatests/com/google/testing/builddefs:GenTestRules.bzl", "GenTestRules")

java_library(
  name="geometry",
  deps=[
    "//accio/java/com/google/common/geometry",
    "//3rdparty/jvm/com/google/guava",
    "//3rdparty/jvm/org/junit/vintage:junit_vintage_engine",
  ],
  srcs=glob(["*.java"]),
)

GenTestRules(
  name = "AllTests",
  default_test_size = "small",
  test_files = glob(["*Test.java"]),
  deps = [":geometry"],
)
