VERSION='2.1.6'

def make(name):
  jar_library(name=name,jars=[scala_jar('com.twitter.finatra', 'finatra-' + name, VERSION)])

make('http')
make('thrift')
make('httpclient')