#!/bin/bash -e
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

set -o nounset

REPO_DIR=/home/vagrant/accio

function upstart_update {
  # Stop and start is necessary to update a the configuration of
  # an upstart job.  We'll rarely change the configuration, but
  # it's probably better to do this upfront and avoid surprises/confusion.
  # Executing true on failure to please bash -e
  sudo systemctl stop $1  || true
  sudo systemctl start $1 || true
}

function build_cli {
  bazel build accio/java/fr/cnrs/liris/lumos/cli:binary
  sudo cp bazel-genfiles/accio/java/fr/cnrs/liris/lumos/cli/cli_binary /usr/local/bin/lumos
}

function build_server {
  bazel build accio/java/fr/cnrs/liris/lumos/server:server_deploy.jar
  sudo cp bazel-bin/accio/java/fr/cnrs/liris/lumos/server/server_deploy.jar /usr/local/bin/lumos-server.jar
  upstart_update lumos-server
}

function build_gateway {
  bazel run @yarn//:yarn
  bazel build accio/java/fr/cnrs/liris/lumos/gateway:gateway_deploy.jar
  sudo cp bazel-bin/accio/java/fr/cnrs/liris/lumos/gateway/gateway_deploy.jar /usr/local/bin/lumos-gateway.jar
  upstart_update lumos-gateway
}

function build_all {
  build_cli
  build_server
  build_gateway
}

function print_components {
  echo 'Please select from: cli, server, gateway, or all.'
}

if [ "$#" -eq 0 ]; then
  echo 'Must specify at least one component to build'
  print_components
  exit 1
fi

# Check that all given components exist, i.e., there is a function to build them.
for component in "$@"; do
  type "build_$component" >/dev/null && continue
  echo "Component $component is unrecognized."
  print_components
  exit 1
done

cd ${REPO_DIR}
update-sources
for component in "$@"; do
  build_$component
done

exit 0
