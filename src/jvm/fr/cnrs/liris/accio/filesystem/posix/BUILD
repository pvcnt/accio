scala_library(
  dependencies=[
    ':annotations',
    'src/jvm/fr/cnrs/liris/accio/filesystem',
    'src/jvm/fr/cnrs/liris/common/util',
    '3rdparty/jvm/com/twitter/inject:core',
  ],
  sources=globs('*.scala')
)

java_library(
  name='annotations',
  dependencies=[
    '3rdparty/jvm/com/twitter/inject:core',
  ],
  sources=globs('*.java')
)