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
  name = 'command',
  srcs = glob(["*.scala"]),
  deps = [
    ':annotations',
    '//accio/java/fr/cnrs/liris/accio/tools/cli/config',
    '//accio/java/fr/cnrs/liris/accio/tools/cli/controller',
    '//accio/java/fr/cnrs/liris/accio/tools/cli/event',
    '//accio/java/fr/cnrs/liris/accio/api',
    '//accio/java/fr/cnrs/liris/accio/reporting',
    '//accio/java/fr/cnrs/liris/accio/util',
    '//accio/java/fr/cnrs/liris/common/io',
    '//accio/java/fr/cnrs/liris/common/util',
    '//accio/java/fr/cnrs/liris/common/flags',
    '//accio/java/fr/cnrs/liris/common/flags:annotations',
    '//accio/java/fr/cnrs/liris/common/reflect',
    '//accio/thrift/fr/cnrs/liris/accio/agent',
    '//3rdparty/jvm/com/twitter:inject_core',
    '//3rdparty/jvm/com/twitter:util_slf4j_api',
    '//3rdparty/jvm/com/twitter:finatra_jackson',
    '//3rdparty/jvm/ch/qos/logback:logback_core',
    '//3rdparty/jvm/ch/qos/logback:logback_classic',
    '//3rdparty/jvm/org/ocpsoft/prettytime',
  ],
  visibility = ["//visibility:public"],
)

java_library(
  name = 'annotations',
  srcs = glob(["*.java"]),
)
