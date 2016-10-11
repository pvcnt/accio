VERSION = '0.9.2'


def make(name):
    jar_library(name=name, jars=[scala_jar('com.twitter', 'bijection-' + name, VERSION)])


make('core')
make('thrift')
make('guava')
make('scrooge')
make('json')
make('util')
