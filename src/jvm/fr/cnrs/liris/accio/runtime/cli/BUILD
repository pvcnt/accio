scala_library(
  dependencies=[
    ':annotations',
    'src/jvm/fr/cnrs/liris/accio/runtime/event',
    'src/jvm/fr/cnrs/liris/common/flags',
    'src/jvm/fr/cnrs/liris/common/reflect',
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