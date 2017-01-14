scala_library(
  name='client',
  dependencies=[
    ':annotations',
    'src/jvm/fr/cnrs/liris/accio/core/domain',
    'src/jvm/fr/cnrs/liris/accio/core/application',
    'src/thrift/fr/cnrs/liris/accio/agent',

    'src/jvm/fr/cnrs/liris/common/util',
    'src/jvm/fr/cnrs/liris/common/flags',
    '3rdparty/jvm/com/typesafe/scala-logging',
    '3rdparty/jvm:scala-guice',
    '3rdparty/jvm:logback',
  ],
  excludes = [
    # Conflicts with scalap coming from common/reflect.
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
    ':client',
  ],
  resources=[
    'src/resources/fr/cnrs/liris/accio/cli',
  ],
  main='fr.cnrs.liris.accio.client.AccioClientMain',
  basename='accio',
)
