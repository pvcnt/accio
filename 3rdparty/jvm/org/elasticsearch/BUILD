VERSION = '5.1.2'

jar_library(
  name='client',
  jars=[jar('org.elasticsearch.client', 'transport', VERSION)]
)

jar_library(
  name='testing',
  jars=[
    jar('org.apache.lucene', 'lucene-test-framework', '6.3.0'),
    jar('org.elasticsearch.test', 'framework', VERSION),
    jar('org.apache.logging.log4j', 'log4j-api', '2.6.2'),
    jar('org.apache.logging.log4j', 'log4j-to-slf4j', '2.6.2'),
  ]
)