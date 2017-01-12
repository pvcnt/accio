java_thrift_library(
  name='domain',
  sources=globs('*.thrift'),
  dependencies=[
    '3rdparty/jvm:scrooge-core',
    '3rdparty/jvm/com/twitter/finagle:thrift',
  ],
  compiler='scrooge',
  language='scala',
  rpc_style = 'finagle',
)