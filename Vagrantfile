# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.configure(2) do |config|
  config.vm.box = "ubuntu/trusty64"

  # Create a forwarded port mapping which allows access to a specific port
  # within the machine from a port on the host machine. In the example below,
  # accessing "localhost:8080" will access port 80 on the guest machine.
  # config.vm.network "forwarded_port", guest: 80, host: 8080

  # Create a private network, which allows host-only access to the machine
  # using a specific IP.
  # config.vm.network "private_network", ip: "192.168.33.10"

  # Create a public network, which generally matched to bridged network.
  # Bridged networks make the machine appear as another physical device on
  # your network.
  # config.vm.network "public_network"

  config.vm.provider "virtualbox" do |vb|
      vb.name = "privamov"
      vb.memory = "1024"
      vb.cpus = 2
    end

  config.vm.provision "shell", inline: <<-SHELL
      sudo add-apt-repository ppa:webupd8team/java -y
      sudo apt-get update
      echo debconf shared/accepted-oracle-license-v1-1 select true | sudo debconf-set-selections
      echo debconf shared/accepted-oracle-license-v1-1 seen true | sudo debconf-set-selections

      DEBIAN_FRONTEND=noninteractive sudo apt-get install -y --force-yes \
        python-dev \
        oracle-java8-installer \
        oracle-java8-set-default \
        nodejs \
        npm \
        php5-cli \
        autoconf \
        automake

      wget https://github.com/facebook/watchman/archive/v4.5.0.tar.gz && tar -xzf v4.5.0.tar.gz
      cd watchman-4.5.0 && ./autogen.sh && ./configure && make && sudo make install
      cd && sudo rm -rf *.tar.gz watchman-*
    SHELL

    #config.vm.synced_folder ".", "/vagrant", type: "rsync",
    #  rsync__exclude: [
    #    ".git/",
    #    ".pants.d/",
    #    ".pids/",
    #    "dist/",
    #    ".pands.workdir.file_lock",
    #    "target/",
    #    "node_modules/",
    #    "vendor/"
    #  ]
end