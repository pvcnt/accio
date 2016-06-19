scala_library(
    name='framework',
    dependencies=[
        ':annotations',
        'src/jvm/fr/cnrs/liris/common/util',
        'src/jvm/fr/cnrs/liris/accio/core/model',
        'src/jvm/fr/cnrs/liris/accio/core/param',
        'src/jvm/fr/cnrs/liris/accio/core/dataset',
    ],
    sources=globs('*.scala')
)

java_library(
    name='annotations',
    sources=globs('*.java')
)
