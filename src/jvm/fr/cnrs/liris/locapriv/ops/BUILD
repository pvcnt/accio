scala_library(
  dependencies=[
    ':resources',
    'src/jvm/fr/cnrs/liris/accio/framework/sdk',
    'src/jvm/fr/cnrs/liris/accio/framework/sdk:annotations',
    'src/jvm/fr/cnrs/liris/locapriv/sparkle',
    'src/jvm/fr/cnrs/liris/locapriv/model',
    'src/jvm/fr/cnrs/liris/locapriv/lppm',
    'src/jvm/fr/cnrs/liris/locapriv/clustering',
    'src/jvm/fr/cnrs/liris/locapriv/io',
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
