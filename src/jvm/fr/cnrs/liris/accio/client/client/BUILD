scala_library(
  dependencies=[
    ':annotations',
    'src/thrift/fr/cnrs/liris/accio/agent',
    'src/jvm/fr/cnrs/liris/common/util',
    '3rdparty/jvm/com/twitter/finatra:jackson',
    '3rdparty/jvm/com/google/guice',
  ],
  sources=globs('*.scala')
)

java_library(
  name='annotations',
  sources=globs('*.java')
)