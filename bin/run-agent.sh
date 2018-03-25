#!/usr/bin/env bash
# Accio is a program whose purpose is to study location privacy.
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

if [ ! -f dist/accio-executor.jar ]; then
  ./pants binary src/jvm/fr/cnrs/liris/accio/executor:bin
fi
tmpdir=/tmp/accio-agent
mkdir -p $tmpdir

./pants run src/jvm/fr/cnrs/liris/accio/agent:bin -- \
  -cluster_name=devcluster \
  -master \
  -worker \
  -admin.port=":9990" \
  -thrift.port=":9999" \
  -executor_uri=$(pwd)/dist/accio-executor.jar \
  -workdir=$tmpdir/tmp \
  -storage.type=memory \
  -filesystem.type=posix \
  -filesystem.posix.root=$tmpdir/fs \
  "$@"
