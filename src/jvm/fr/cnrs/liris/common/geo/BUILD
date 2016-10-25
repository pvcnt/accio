scala_library(
  name='geo',
  dependencies=[
    'src/jvm/com/google/common/geometry',
  ],
  sources=['BoundingBox.scala', 'Location.scala', 'Distance.scala', 'conversions.scala']
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
