scala_library(
  name='geo',
  dependencies=[
    'src/jvm/com/google/common/geometry',
    'src/jvm/fr/cnrs/liris/common/util',
  ],
  sources=['Mercator.scala', 'BoundingBox.scala', 'Location.scala']
)

scala_library(
  name='json',
  dependencies=[
    ':geo',
    '3rdparty/jvm/com/fasterxml/jackson:annotations',
    '3rdparty/jvm/com/fasterxml/jackson:scala',
  ],
  sources=['GeoJson.scala']
)
