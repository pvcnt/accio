scala_library(
  dependencies=[
    'src/jvm/fr/cnrs/liris/accio/framework/api',
    'src/jvm/fr/cnrs/liris/common/util',
    'src/thrift/fr/cnrs/liris/accio/agent',
    '3rdparty/jvm/com/twitter/finatra:http',
    '3rdparty/jvm/com/twitter/finatra:httpclient',
    '3rdparty/jvm/com/twitter/finagle:serversets',
    '3rdparty/jvm:logback',
  ],
  excludes = [
    # Multiple logging frameworks.
    exclude('org.slf4j', 'slf4j-log4j12'),
    exclude('org.slf4j', 'slf4j-jdk14'),
    exclude('log4j', 'log4j'),
    exclude('commons-logging', 'commons-logging'),
    # We already include canonical Thrift library, we do not need the one provided by Finagle.
    exclude('com.twitter', 'libthrift'),
  ],
  resources = [
    'src/resources/fr/cnrs/liris/accio/gateway',
    'src/node/fr/cnrs/liris/accio/gateway',
  ],
  sources=globs('*.scala')
)

jvm_binary(
  name='bin',
  dependencies=[
    ':gateway',
  ],
  main='fr.cnrs.liris.accio.gateway.GatewayServerMain',
  basename='accio-gateway',
)