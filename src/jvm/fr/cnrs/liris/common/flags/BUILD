scala_library(
    name='flags',
    dependencies=[
        '3rdparty/jvm/twitter/util:core',
        ':flags-java',
        'src/jvm/fr/cnrs/liris/common/util',
        'src/jvm/fr/cnrs/liris/common/reflect',
    ],
    sources=globs('*.scala')
)

java_library(
    name='flags-java',
    sources=globs('*.java')
)