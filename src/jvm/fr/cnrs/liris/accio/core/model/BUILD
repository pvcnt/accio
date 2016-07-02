scala_library(
    name='model',
    dependencies=[
        '3rdparty/jvm:scala-time',
        '3rdparty/jvm:breeze',
        'src/jvm/fr/cnrs/liris/common/geo',
        'src/jvm/fr/cnrs/liris/common/util',
    ],
    sources=['Poi.scala', 'PoiSet.scala', 'Event.scala', 'Trace.scala']
)

scala_library(
    name='json',
    dependencies=[
        ':model',
        'src/jvm/fr/cnrs/liris/common/geo:json',
    ],
    sources=['GeoJsonConverters.scala']
)
