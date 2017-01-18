scala_library(
  name='executor',
  dependencies=[
    'src/jvm/fr/cnrs/liris/accio/core/domain',
    'src/jvm/fr/cnrs/liris/accio/core/service',
    'src/jvm/fr/cnrs/liris/accio/core/infra/downloader',
    'src/jvm/fr/cnrs/liris/accio/core/infra/uploader/local',
    'src/thrift/fr/cnrs/liris/accio/core/domain',
    'src/thrift/fr/cnrs/liris/accio/agent',
    'src/jvm/fr/cnrs/liris/privamov/ops',

    '3rdparty/jvm/com/typesafe/scala-logging',
    '3rdparty/jvm/com/twitter/inject:app',
    '3rdparty/jvm:scala-guice',
  ],
  excludes = [
    # Finagle comes with its own Thrift library.
    exclude(org = 'com.twitter', name = 'libthrift'),
  ],
  sources=globs('*.scala')
)

jvm_binary(
  name='bin',
  dependencies=[
    ':executor',
    '3rdparty/jvm:logback',
  ],
  main='fr.cnrs.liris.accio.executor.AccioExecutorMain',
  basename='accio-executor',
)