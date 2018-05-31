#!/bin/bash -ex
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

# This script is entirely executed as the root user by Vagrant.

BAZEL_VERSION=0.11.0

function install_packages() {
  # Enable user namespaces for Bazel.
  sysctl kernel.unprivileged_userns_clone=1
  echo 'kernel.unprivileged_userns_clone=1' | tee /etc/sysctl.d/99-enable-user-namespaces.conf

  # Install Java and NodeJS.
  apt-get update && apt-get install -y openjdk-8-jdk nodejs-legacy

  # Install Bazel and its dependencies.
  apt-get install -y pkg-config zip g++ zlib1g-dev unzip python
  wget -nv -O install.sh "https://github.com/bazelbuild/bazel/releases/download/${BAZEL_VERSION}/bazel-${BAZEL_VERSION}-installer-linux-x86_64.sh"
  chmod +x install.sh
  sudo ./install.sh
  rm -f install.sh
}

function prepare_extras() {
  # Include build script in default PATH.
  ln -sf /home/vagrant/accio/etc/vagrant/acciobuild.sh /usr/local/bin/acciobuild
  ln -sf /home/vagrant/accio/etc/vagrant/lumosbuild.sh /usr/local/bin/lumosbuild
  chown vagrant: /usr/local/bin/acciobuild && chmod +x /usr/local/bin/acciobuild
  chown vagrant: /usr/local/bin/lumosbuild && chmod +x /usr/local/bin/lumosbuild

  # Copy default clusters.json files.
  mkdir -p /etc/accio && cp /home/vagrant/accio/etc/vagrant/accio/clusters.json /etc/accio
  mkdir -p /etc/lumos && cp /home/vagrant/accio/etc/vagrant/lumos/clusters.json /etc/lumos
}

function prepare_sources {
  mkdir -p /home/vagrant/accio
  ln -sf /vagrant/etc/vagrant/update-sources.sh /usr/local/bin/update-sources
  chmod +x /usr/local/bin/update-sources
  update-sources > /dev/null
  chown -R vagrant: /home/vagrant/accio
}

install_packages
prepare_sources
prepare_extras
su vagrant -c "acciobuild all; lumosbuild all"
