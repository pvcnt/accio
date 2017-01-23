scala_library(
  dependencies=[
    'src/jvm/fr/cnrs/liris/accio/core/domain',
    'src/jvm/fr/cnrs/liris/accio/core/infra/cli',
    'src/jvm/fr/cnrs/liris/accio/client/command',
    'src/jvm/fr/cnrs/liris/accio/client/service',
    'src/thrift/fr/cnrs/liris/accio/agent',

    '3rdparty/jvm/com/typesafe/scala-logging',
    '3rdparty/jvm:scala-guice',
    '3rdparty/jvm:logback',
  ],
  excludes = [
    # common/reflect comes with its own Scala compiler library, we do not need the one provided by Finatra.
    exclude(org='com.twitter.finatra', name='finatra-scalap-compiler-deps_2.11'),
    # Finagle comes with its own Thrift library.
    exclude(org = 'com.twitter', name = 'libthrift'),
  ],
  sources=globs('*.scala')
)

jvm_binary(
  name='bin',
  dependencies=[
    ':client',
  ],
  resources=[
    'src/resources/fr/cnrs/liris/accio/client',
  ],
  main='fr.cnrs.liris.accio.client.AccioClientMain',
  basename='accio-client',
)
