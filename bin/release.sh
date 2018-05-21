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

DIST_DIR=dist

mkdir -p $DIST_DIR

bazel build accio/java/fr/cnrs/liris/accio/agent:agent_deploy.jar
bazel build accio/java/fr/cnrs/liris/accio/executor:executor_deploy.jar
bazel build accio/java/fr/cnrs/liris/accio/tools/gateway:gateway_deploy.jar
bazel build accio/java/fr/cnrs/liris/accio/cli:binary

cp bazel-bin/accio/java/fr/cnrs/liris/accio/agent/agent_deploy.jar $DIST_DIR/accio-agent.jar
cp bazel-bin/accio/java/fr/cnrs/liris/accio/executor/executor_deploy.jar $DIST_DIR/accio-executor.jar
cp bazel-bin/accio/java/fr/cnrs/liris/accio/tools/gateway/gateway_deploy.jar $DIST_DIR/accio-gateway.jar
cp bazel-genfiles/accio/java/fr/cnrs/liris/accio/cli/cli_binary $DIST_DIR/accio

echo "Release artifacts are available in $DIST_DIR"