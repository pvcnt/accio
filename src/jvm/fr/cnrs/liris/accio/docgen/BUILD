scala_library(
  name='docgen',
  dependencies=[
    'src/jvm/fr/cnrs/liris/accio/core/framework',
    'src/jvm/fr/cnrs/liris/privamov/ops',
    'src/jvm/fr/cnrs/liris/common/flags',
    '3rdparty/jvm/com/typesafe/scala-logging',
    '3rdparty/jvm:scala-guice',
  ],
  excludes = [
    exclude(org='com.twitter.finatra', name='finatra-scalap-compiler-deps_2.11')
  ],
  sources=globs('*.scala')
)

jvm_binary(
  name='bin',
  dependencies=[
    ':docgen',
    '3rdparty/jvm:logback',
  ],
  resources=[
    'src/resources/fr/cnrs/liris/accio/cli',
  ],
  main='fr.cnrs.liris.accio.docgen.AccioDocgenMain',
  basename='accio-docgen',
)
