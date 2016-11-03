scala_library(
  name='cli',
  dependencies=[
    ':annotations',
    'src/jvm/fr/cnrs/liris/accio/core/framework',
    'src/jvm/fr/cnrs/liris/accio/core/runtime',
    'src/jvm/fr/cnrs/liris/privamov/ops',
    'src/jvm/fr/cnrs/liris/common/util',
    'src/jvm/fr/cnrs/liris/common/reflect',
    'src/jvm/fr/cnrs/liris/common/flags',
    'src/jvm/fr/cnrs/liris/common/stats',
    '3rdparty/jvm/com/typesafe/scala-logging',
    '3rdparty/jvm:scala-guice',
    '3rdparty/jvm:logback',
  ],
  excludes = [
    exclude(org='com.twitter.finatra', name='finatra-scalap-compiler-deps_2.11')
  ],
  sources=globs('*.scala')
)

java_library(
  name='annotations',
  sources=globs('*.java')
)

jvm_binary(
  name='bin',
  dependencies=[
    ':cli',
  ],
  resources=[
    'src/resources/fr/cnrs/liris/accio/cli',
  ],
  main='fr.cnrs.liris.accio.cli.AccioAppMain',
  basename='accio',
)
