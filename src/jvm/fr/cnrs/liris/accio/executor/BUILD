scala_library(
  dependencies=[
    'src/jvm/fr/cnrs/liris/accio/api',
    'src/jvm/fr/cnrs/liris/accio/service',
    'src/jvm/fr/cnrs/liris/accio/discovery/inject',
    'src/jvm/fr/cnrs/liris/accio/filesystem/inject',
    'src/jvm/fr/cnrs/liris/accio/util',
    'src/jvm/fr/cnrs/liris/accio/util:annotations',
    'src/jvm/fr/cnrs/liris/accio/runtime/finagle',
    'src/thrift/fr/cnrs/liris/accio/agent',
    scoped('src/jvm/fr/cnrs/liris/locapriv/ops', 'runtime'),
    '3rdparty/jvm/com/twitter/inject:app',
  ],
  excludes = [
    # Multiple logging frameworks.
    exclude('commons-logging', 'commons-logging'),
    # common/reflect comes with its own Scala compiler library, we do not need the one provided by Finatra.
    exclude('com.twitter.finatra', 'finatra-scalap-compiler-deps_2.11'),
    # We already include canonical Thrift library, we do not need the one provided by Finagle.
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