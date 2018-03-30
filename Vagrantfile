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

Vagrant.configure(2) do |config|
  # Ubuntu 16.04 LTS.
  config.vm.box = "ubuntu/xenial64"
  config.vm.box_version = "20180323.0.0"

  # Create a private network.
  config.vm.network "private_network", ip: "192.168.50.4"

  # Configure Virtualbox.
  config.vm.provider "virtualbox" do |vb|
      vb.name = "accio.dev"
      vb.memory = "4096"
      vb.cpus = 2
    end

  # Initial provisioning.
  config.vm.provision "shell" do |s|
    s.path = "etc/vagrant/provision-dev-cluster.sh"
    s.binary = true
  end
end
