jar_library(
  name='junit',
  jars=[jar('junit', 'junit', '4.12')]
)

jar_library(
  name='scalatest',
  jars=[
    scala_jar('org.scalatest', 'scalatest', '2.2.6'),
    scala_jar('org.scalamock', 'scalamock-scalatest-support', '3.2.2'),
  ],
  dependencies=[
    ':junit',
  ]
)

jar_library(
  name='findbugs',
  jars=[jar('com.google.code.findbugs', 'jsr305', '3.0.1')]
)

jar_library(
  name='scala-guice',
  jars=[scala_jar('net.codingwell', 'scala-guice', '4.0.1')]
)

jar_library(
  name='logback',
  jars=[jar('ch.qos.logback', 'logback-classic', '1.1.5')]
)

jar_library(
  name='scala-time',
  jars=[scala_jar('com.github.nscala-time', 'nscala-time', '2.12.0')]
)

jar_library(
  name='thrift',
  jars=[jar('org.apache.thrift', 'libthrift', '0.9.2')]
)

jar_library(
  name='scrooge-core',
  jars=[scala_jar('com.twitter', 'scrooge-core', '4.6.0')],
  dependencies=[
    ':thrift',
  ]
)