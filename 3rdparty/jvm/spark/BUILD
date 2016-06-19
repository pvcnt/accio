VERSION='1.6.1'

def make(name):
  jar_library(name=name, jars=[scala_jar('org.apache.spark', 'spark-' + name, VERSION)])

make('core')
make('sql')
make('streaming')
make('streaming-kafka')
make('mllib')