junit_tests(
  dependencies=[
    ':resources',
    'src/jvm/fr/cnrs/liris/accio/framework/filesystem/archive',
    'test/jvm/fr/cnrs/liris/testing',
  ],
  sources=globs('*.scala'),
)

resources(
  name='resources',
  sources=globs('*.bz2', '*.gz', '*.zip', '*.tar'),
)