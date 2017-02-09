java_thrift_library(
  sources=globs('*.thrift'),
  dependencies=[
    'src/thrift/fr/cnrs/liris/dal/core/api',
    '3rdparty/jvm:scrooge-core',
    '3rdparty/jvm/com/twitter/finagle:thrift',
  ],
  compiler='scrooge',
  language='scala',
  rpc_style = 'finagle',
)