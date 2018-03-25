scala_library(
  dependencies=[
    ':resources',
    'src/jvm/fr/cnrs/liris/accio/ops/model',
    'src/jvm/fr/cnrs/liris/accio/ops/io',
    'src/jvm/fr/cnrs/liris/common/geo',
    '3rdparty/jvm:scala-time',
  ],
  sources=globs('*.scala'),
)

resources(
  name='resources',
  sources=['abboip.csv'],
)