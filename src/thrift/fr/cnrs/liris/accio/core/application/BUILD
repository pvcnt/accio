java_thrift_library(
  name='application',
  sources=globs('*.thrift'),
  dependencies=[
    'src/thrift/fr/cnrs/liris/accio/core/domain',
    '3rdparty/jvm:scrooge-core',
    '3rdparty/jvm/com/twitter/finagle:thrift',
  ],
  compiler='scrooge',
  language='scala',
  rpc_style = 'finagle',
)