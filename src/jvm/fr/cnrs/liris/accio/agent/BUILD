scala_library(
  dependencies=[
    'src/jvm/fr/cnrs/liris/accio/agent/handler',
    'src/jvm/fr/cnrs/liris/accio/core/filesystem/inject',
    'src/jvm/fr/cnrs/liris/accio/core/storage/inject',
    'src/jvm/fr/cnrs/liris/accio/core/scheduler/inject',
    'src/jvm/fr/cnrs/liris/accio/core/dsl/inject',
    'src/jvm/fr/cnrs/liris/accio/core/framework',
    'src/jvm/fr/cnrs/liris/accio/runtime/finagle',
    'src/jvm/fr/cnrs/liris/accio/core/util:annotations',
    'src/jvm/fr/cnrs/liris/accio/runtime/commandbus',
    'src/jvm/fr/cnrs/liris/accio/runtime/logging',
    'src/jvm/fr/cnrs/liris/privamov/ops',
    'src/thrift/fr/cnrs/liris/accio/agent',
    '3rdparty/jvm/com/twitter/finatra:thrift',
    '3rdparty/jvm/com/twitter/finagle:serversets',
  ],
  resources = [
    'src/resources/fr/cnrs/liris/accio/agent',
  ],
  excludes = [
    # Multiple logging frameworks.
    exclude('org.slf4j', 'slf4j-log4j12'),
    exclude('org.slf4j', 'slf4j-jdk14'),
    exclude('log4j', 'log4j'),
    exclude('commons-logging', 'commons-logging'),
    # We already include canonical Thrift library, we do not need the one provided by Finagle.
    exclude('com.twitter', 'libthrift'),
    # Multiple inclusions of Netty through netty-all and netty-*.
    exclude('io.netty', 'netty-buffer'),
    exclude('io.netty', 'netty-common'),
    exclude('io.netty', 'netty-handler'),
    exclude('io.netty', 'netty-handler-proxy'),
    exclude('io.netty', 'netty-transport'),
    exclude('io.netty', 'netty-resolver'),
    exclude('io.netty', 'netty-codec'),
    exclude('io.netty', 'netty-codec-http'),
    exclude('io.netty', 'netty-codec-socks'),
    # common/reflect comes with its own Scala compiler library, we do not need the one provided by Finatra.
    exclude('com.twitter.finatra', 'finatra-scalap-compiler-deps_2.11'),
  ],
  sources=globs('*.scala')
)

jvm_binary(
  name='bin',
  dependencies=[
    ':agent',
  ],
  main='fr.cnrs.liris.accio.agent.AgentServerMain',
  basename='accio-agent',
)