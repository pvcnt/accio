scala_library(
  name='docgen',
  dependencies=[
    'src/jvm/fr/cnrs/liris/accio/framework/discovery/inject',
    scoped('src/jvm/fr/cnrs/liris/accio/ops', 'runtime'),
    '3rdparty/jvm/com/twitter/inject:app',
  ],
  excludes = [
    exclude(org='com.twitter.finatra', name='finatra-scalap-compiler-deps_2.11')
  ],
  sources=globs('*.scala')
)

jvm_binary(
  name='bin',
  dependencies=[
    ':docgen',
    'src/resources/fr/cnrs/liris/accio/tools/cli',
    '3rdparty/jvm:logback',
  ],
  main='fr.cnrs.liris.accio.tools.docgen.AccioDocgenMain',
  basename='accio-docgen',
)
