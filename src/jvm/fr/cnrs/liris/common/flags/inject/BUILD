scala_library(
  name='inject',
  dependencies=[
    ':annotations',
    'src/jvm/fr/cnrs/liris/common/flags',
    '3rdparty/jvm/com/google/guice',
    '3rdparty/jvm/com/typesafe/scala-logging',
  ],
  java_sources=[
    ':annotations',
  ],
  sources=globs('*.scala')
)

java_library(
  name='annotations',
  dependencies = [
    '3rdparty/jvm/com/google/guice',
  ],
  sources=globs('*.java')
)
