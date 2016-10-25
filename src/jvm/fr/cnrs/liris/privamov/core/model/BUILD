scala_library(
  name='model',
  dependencies=[
    'src/jvm/fr/cnrs/liris/common/geo',
    '3rdparty/jvm/com/google/guava',
    '3rdparty/jvm:scala-time',
    '3rdparty/jvm/org/scalanlp:breeze',
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
