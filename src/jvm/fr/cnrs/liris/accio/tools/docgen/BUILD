scala_library(
  name='docgen',
  dependencies=[
    ':resources',
    'src/jvm/fr/cnrs/liris/accio/discovery/inject',
    '3rdparty/jvm:logback',
    scoped('src/jvm/fr/cnrs/liris/locapriv/ops', 'runtime'),
    '3rdparty/jvm/com/twitter/inject:app',
  ],
  excludes = [
    exclude(org='com.twitter.finatra', name='finatra-scalap-compiler-deps_2.11')
  ],
  sources=globs('*.scala')
)

resources(
  name='resources',
  sources=['logback.xml']
)

jvm_binary(
  name='bin',
  dependencies=[':docgen'],
  main='fr.cnrs.liris.accio.tools.docgen.AccioDocgenMain',
  basename='accio-docgen',
)
