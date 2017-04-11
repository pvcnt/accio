scala_library(
  dependencies=[
    'src/jvm/fr/cnrs/liris/accio/core/api',
    'src/jvm/fr/cnrs/liris/accio/core/framework',
    'src/jvm/fr/cnrs/liris/accio/core/filesystem/inject',
    'src/jvm/fr/cnrs/liris/accio/core/util',
    'src/jvm/fr/cnrs/liris/accio/core/util:annotations',
    'src/thrift/fr/cnrs/liris/accio/agent',
    'src/jvm/fr/cnrs/liris/privamov/ops',
    '3rdparty/jvm/com/twitter/inject:app',
  ],
  excludes = [
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