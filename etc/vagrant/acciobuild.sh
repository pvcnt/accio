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
  bazel build accio/java/fr/cnrs/liris/accio/cli:binary
  sudo cp bazel-genfiles/accio/java/fr/cnrs/liris/accio/cli/cli_binary /usr/local/bin/accio
}

function build_server {
  if [ ! -f /var/lib/accio/executor.jar ]; then
    build_executor
  fi
  if [ ! -f /var/lib/accio/ops/locapriv.jar ]; then
    build_ops
  fi
  bazel build accio/java/fr/cnrs/liris/accio/server:server_deploy.jar
  sudo cp bazel-bin/accio/java/fr/cnrs/liris/accio/server/server_deploy.jar /usr/local/bin/accio-server.jar
  upstart_update accio-server
}

function build_executor {
  bazel build accio/java/fr/cnrs/liris/accio/executor:executor_deploy.jar
  sudo mkdir -p /var/lib/accio
  sudo cp bazel-bin/accio/java/fr/cnrs/liris/accio/executor/executor_deploy.jar /var/lib/accio/executor.jar
}

function build_ops {
  bazel build accio/java/fr/cnrs/liris/locapriv/ops:ops_deploy.jar
  sudo mkdir -p /var/lib/accio/ops
  sudo cp bazel-bin/accio/java/fr/cnrs/liris/locapriv/ops/ops_deploy.jar /var/lib/accio/ops/locapriv.jar
}

function build_all {
  build_cli
  build_ops
  build_executor
  build_server
}

function print_components {
  echo 'Please select from: cli, executor, server, ops, or all.'
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
