scala_library(
  dependencies=[
    ':resources',
    'src/jvm/fr/cnrs/liris/locapriv/model',
    'src/jvm/fr/cnrs/liris/locapriv/io',
    'src/jvm/fr/cnrs/liris/common/geo',
    '3rdparty/jvm:scala-time',
  ],
  sources=globs('*.scala'),
)

resources(
  name='resources',
  sources=['abboip.csv'],
)