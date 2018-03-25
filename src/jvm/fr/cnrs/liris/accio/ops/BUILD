scala_library(
  dependencies=[
    ':resources',
    'src/jvm/fr/cnrs/liris/accio/framework/sdk',
    'src/jvm/fr/cnrs/liris/accio/framework/sdk:annotations',
    'src/jvm/fr/cnrs/liris/accio/ops/sparkle',
    'src/jvm/fr/cnrs/liris/accio/ops/model',
    'src/jvm/fr/cnrs/liris/accio/ops/lppm',
    'src/jvm/fr/cnrs/liris/accio/ops/clustering',
    'src/jvm/fr/cnrs/liris/accio/ops/io',
    'src/jvm/fr/cnrs/liris/common/geo',
    'src/jvm/fr/cnrs/liris/common/util',
    'src/jvm/fr/cnrs/liris/common/random',
    'src/jvm/com/google/common/geometry',
    '3rdparty/jvm:scala-time',
    '3rdparty/jvm/org/scalanlp:breeze',
    '3rdparty/jvm/com/twitter/util:jvm',
  ],
  sources=globs('*.scala'),
)

resources(
  name='resources',
  sources=['linux_w4m_LST', 'linux_w4m_LST_chunk', 'mac_w4m_LST', 'mac_w4m_LST_chunk']
)
