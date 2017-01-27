# Accio is a program whose purpose is to study location privacy.
# Copyright (C) 2016-2017 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

#!/bin/bash -ex

function install_packages() {
  curl -sS https://dl.yarnpkg.com/debian/pubkey.gpg | sudo apt-key add -
  echo "deb https://dl.yarnpkg.com/debian/ stable main" | sudo tee /etc/apt/sources.list.d/yarn.list
  DEBIAN_FRONTEND=noninteractive sudo apt-get install -y --force-yes \
        python-dev \
        libffi-dev \
        openjdk-8-jdk-headless \
        nodejs \
        nodejs-legacy \
        npm \
        yarn \
        autoconf \
        automake \
        htop
  sudo npm install -g browserify watchify
}

function prepare_extras() {
  # Include build script in default PATH.
  ln -sf /home/ubuntu/accio/etc/vagrant/acciobuild.sh /usr/local/bin/acciobuild
  chown ubuntu: /usr/local/bin/acciobuild && chmod +x /usr/local/bin/acciobuild

  # Install Pants bash completion. It will trigger Pants initialization (including download).
  su ubuntu -c "./pants bash-completion" > /etc/bash_completion.d/pants-completion.bash
}

function prepare_sources {
  mkdir -p /home/ubuntu/accio
  ln -sf /vagrant/etc/vagrant/update-sources.sh /usr/local/bin/update-sources
  chmod +x /usr/local/bin/update-sources
  update-sources > /dev/null
  chown -R ubuntu: /home/ubuntu/accio
}

install_packages
prepare_sources
prepare_extras
su ubuntu -c "acciobuild all"
