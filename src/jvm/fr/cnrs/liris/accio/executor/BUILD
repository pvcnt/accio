scala_library(
  dependencies=[
    'src/jvm/fr/cnrs/liris/accio/framework/api',
    'src/jvm/fr/cnrs/liris/accio/framework/service',
    'src/jvm/fr/cnrs/liris/accio/framework/discovery/inject',
    'src/jvm/fr/cnrs/liris/accio/framework/filesystem/inject',
    'src/jvm/fr/cnrs/liris/accio/framework/util',
    'src/jvm/fr/cnrs/liris/accio/framework/util:annotations',
    'src/thrift/fr/cnrs/liris/accio/agent',
    scoped('src/jvm/fr/cnrs/liris/accio/ops', 'runtime'),
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