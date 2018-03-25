scala_library(
  dependencies=[
    ':resources',
    'src/jvm/fr/cnrs/liris/accio/agent/handler',
    'src/jvm/fr/cnrs/liris/accio/filesystem/inject',
    'src/jvm/fr/cnrs/liris/accio/storage/inject',
    'src/jvm/fr/cnrs/liris/accio/scheduler/inject',
    'src/jvm/fr/cnrs/liris/accio/discovery/inject',
    'src/jvm/fr/cnrs/liris/accio/dsl/inject',
    'src/jvm/fr/cnrs/liris/accio/service',
    'src/jvm/fr/cnrs/liris/accio/util:annotations',
    'src/jvm/fr/cnrs/liris/accio/runtime/commandbus',
    'src/jvm/fr/cnrs/liris/accio/runtime/logging',
    'src/jvm/fr/cnrs/liris/accio/runtime/finagle',
    scoped('src/jvm/fr/cnrs/liris/locapriv/ops', 'runtime'),
    'src/thrift/fr/cnrs/liris/accio/agent',
    '3rdparty/jvm/com/twitter/finatra:thrift',
  ],
  excludes = [
    # Multiple logging frameworks.
    exclude('org.slf4j', 'slf4j-log4j12'),
    exclude('org.slf4j', 'slf4j-jdk14'),
    exclude('log4j', 'log4j'),
    exclude('commons-logging', 'commons-logging'),
    # We already include canonical Thrift library, we do not need the one provided by Finagle.
    exclude('com.twitter', 'libthrift'),
    # common/reflect comes with its own Scala compiler library, we do not need the one provided by Finatra.
    exclude('com.twitter.finatra', 'finatra-scalap-compiler-deps_2.11'),
  ],
  sources=globs('*.scala')
)

resources(
  name='resources',
  sources=['logback.xml']
)

jvm_binary(
  name='bin',
  dependencies=[':agent'],
  main='fr.cnrs.liris.accio.agent.AgentServerMain',
  basename='accio-agent',
)
