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

Vagrant.configure(2) do |config|
  # Ubuntu 16.04 LTS.
  config.vm.box = "ubuntu/xenial64"

  # Forward agent's ports.
  config.vm.network "forwarded_port", guest: 8880, host: 8880
  config.vm.network "forwarded_port", guest: 8888, host: 8888

  # Forward gateway's ports.
  config.vm.network "forwarded_port", guest: 9990, host: 9990
  config.vm.network "forwarded_port", guest: 9999, host: 9999

  # Configure Virtualbox.
  config.vm.provider "virtualbox" do |vb|
      vb.name = "accio.local"
      vb.memory = "4096"
      vb.cpus = 4
    end

  # Initial provisioning.
  config.vm.provision "shell", path: "etc/vagrant/provision-dev-cluster.sh"
end