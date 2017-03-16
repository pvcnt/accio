scala_library(
  dependencies=[
    ':annotations',
    'src/jvm/fr/cnrs/liris/accio/client/event',
    'src/jvm/fr/cnrs/liris/common/flags',
    '3rdparty/jvm/com/typesafe/scala-logging',
    '3rdparty/jvm:scala-guice',
    '3rdparty/jvm:logback',
    '3rdparty/jvm/org/joda/time',
  ],
  sources=globs('*.scala')
)

java_library(
  name='annotations',
  sources=globs('*.java')
)