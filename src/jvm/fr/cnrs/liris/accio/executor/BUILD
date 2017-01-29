scala_library(
  dependencies=[
    'src/jvm/fr/cnrs/liris/accio/core/domain',
    'src/jvm/fr/cnrs/liris/accio/core/runtime',
    'src/jvm/fr/cnrs/liris/accio/core/downloader',
    'src/jvm/fr/cnrs/liris/accio/core/downloader/inject',
    'src/jvm/fr/cnrs/liris/accio/core/uploader',
    'src/jvm/fr/cnrs/liris/accio/core/uploader/inject',
    'src/thrift/fr/cnrs/liris/accio/core/domain',
    'src/thrift/fr/cnrs/liris/accio/agent',
    'src/jvm/fr/cnrs/liris/privamov/ops',

    '3rdparty/jvm/com/typesafe/scala-logging',
    '3rdparty/jvm/com/twitter/inject:app',
  ],
  excludes = [
    # common/reflect comes with its own Scala compiler library, we do not need the one provided by Finatra.
    exclude('com.twitter.finatra', 'finatra-scalap-compiler-deps_2.11'),
    # We already include canonical Thrift library, we do not need the one provided by Finagle.
    exclude(org = 'com.twitter', name = 'libthrift'),
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