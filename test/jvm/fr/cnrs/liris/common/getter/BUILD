junit_tests(
  name='getter',
  dependencies=[
    'src/jvm/fr/cnrs/liris/common/getter',
    'test/jvm/fr/cnrs/liris/testing',
  ],
  resources = [
    ':archives',
  ],
  sources=globs('*.scala'),
)

resources(
  name='archives',
  sources=globs('*.zip', '*.gz', '*.bz2', '*.tar')
)