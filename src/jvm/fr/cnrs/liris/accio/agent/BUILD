scala_library(
  name='agent',
  dependencies=[
    'src/jvm/fr/cnrs/liris/accio/core/application/handler',
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