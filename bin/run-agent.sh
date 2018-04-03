#!/usr/bin/env bash
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

bazel build accio/java/fr/cnrs/liris/accio/executor:executor_deploy.jar
mkdir -p /tmp/accio-agent

bazel build accio/java/fr/cnrs/liris/accio/agent
./bazel-bin/accio/java/fr/cnrs/liris/accio/agent/agent \
  -cluster_name=devcluster \
  -admin.port=":9990" \
  -thrift.port=":9999" \
  -scheduler=local \
  -force_scheduling \
  -executor_uri=$(pwd)/bazel-bin/accio/java/fr/cnrs/liris/accio/executor/executor_deploy.jar \
  -datadir=/tmp/accio-agent \
  -storage=memory \
  "$@"
