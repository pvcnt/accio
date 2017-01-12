scala_library(
  name='executor',
  dependencies=[
    'src/jvm/fr/cnrs/liris/accio/core/api',
    'src/jvm/fr/cnrs/liris/accio/core/domain',
    'src/jvm/fr/cnrs/liris/accio/core/application',
    'src/jvm/fr/cnrs/liris/accio/core/infrastructure',
    'src/jvm/fr/cnrs/liris/accio/core/infrastructure/uploader/local',
    'src/thrift/fr/cnrs/liris/accio/core/domain',
    'src/thrift/fr/cnrs/liris/accio/agent',

    'src/jvm/fr/cnrs/liris/common/flags',
    'src/jvm/fr/cnrs/liris/common/flags/inject',
    'src/jvm/fr/cnrs/liris/privamov/ops',

    '3rdparty/jvm/com/typesafe/scala-logging',
    '3rdparty/jvm:scala-guice',
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