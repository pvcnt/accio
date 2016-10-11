VERSION='6.30.0'

def make(name):
  jar_library(name=name, jars=[scala_jar('com.twitter', 'util-' + name, VERSION)])

make('core')
make('eval')
make('cache')
make('reflect')
make('collection')