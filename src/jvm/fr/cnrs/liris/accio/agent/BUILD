scala_library(
  name='agent',
  dependencies=[
    'src/jvm/fr/cnrs/liris/accio/core/service/handler',
    'src/jvm/fr/cnrs/liris/accio/core/infra/jackson',
    'src/jvm/fr/cnrs/liris/accio/core/infra/downloader',
    'src/jvm/fr/cnrs/liris/accio/core/infra/scheduler/local',
    'src/jvm/fr/cnrs/liris/accio/core/infra/uploader/local',
    'src/jvm/fr/cnrs/liris/accio/core/infra/statemgr/zookeeper',
    'src/jvm/fr/cnrs/liris/accio/core/infra/statemgr/local',
    'src/jvm/fr/cnrs/liris/accio/core/infra/storage/elastic',
    'src/jvm/fr/cnrs/liris/accio/core/infra/storage/local',
    'src/jvm/fr/cnrs/liris/privamov/ops',
    'src/thrift/fr/cnrs/liris/accio/agent',
    '3rdparty/jvm/com/twitter/finatra:thrift',
    '3rdparty/jvm/com/twitter/finagle:serversets',
    '3rdparty/jvm:logback',
  ],
  excludes = [
    exclude(org = 'org.slf4j', name = 'slf4j-log4j12'),
    exclude(org = 'org.slf4j', name = 'slf4j-jdk14'),
  ],
  resources = [
    'src/resources/fr/cnrs/liris/accio/agent',
  ],
  sources=globs('*.scala')
)

jvm_binary(
  name='bin',
  dependencies=[
    ':agent',
  ],
  main='fr.cnrs.liris.accio.agent.AccioAgentMain',
  basename='accio-agent',
)