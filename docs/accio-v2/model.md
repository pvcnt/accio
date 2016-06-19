Event
  * source: string
  * trace: string
  * capture_time: timestamp
  * arrival_time: timestamp
  * location: LatLng
  * properties: Map[String, Double]

Trace:
  * id: string
  * events: Seq[Event]

LatLng:
  * lat: double
  * lng: double