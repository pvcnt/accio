java_thrift_library(
  name='agent',
  sources=globs('*.thrift'),
  dependencies=[
    'src/thrift/fr/cnrs/liris/accio/core/service',
    '3rdparty/jvm:scrooge-core',
    '3rdparty/jvm/com/twitter/util:core',
    '3rdparty/jvm/com/twitter/finagle:thrift',
  ],
  compiler='scrooge',
  language='scala',
  rpc_style = 'finagle',
)