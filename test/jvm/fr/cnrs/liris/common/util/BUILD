junit_tests(
    name='util',
    dependencies=[
        'src/jvm/fr/cnrs/liris/common/util',
        'test/jvm/fr/cnrs/liris/testing',
        ':messages',
    ],
    sources=globs('*.scala'),
)

resources(
  name='messages',
  sources=globs('*.message')
)
