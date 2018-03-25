junit_tests(
  dependencies=[
    ':resources',
    'src/jvm/fr/cnrs/liris/accio/agent/handler',
    'test/jvm/fr/cnrs/liris/testing',
    '3rdparty/jvm/com/twitter/util:core',
  ],
  sources=globs('*.scala'),
)

resources(
  name='resources',
  sources=['accio-dummy-executor.jar']
)