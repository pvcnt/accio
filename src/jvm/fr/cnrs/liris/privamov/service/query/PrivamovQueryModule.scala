package fr.cnrs.liris.privamov.service.query

import com.google.inject.{Provides, Singleton}
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import com.twitter.inject.TwitterModule

object PrivamovQueryModule extends TwitterModule {
  val elasticUri = flag("elastic_uri", "localhost:9300", "Elasticsearch URI")

  @Provides
  @Singleton
  def providesElasticClient(): ElasticClient = {
    ElasticClient.transport(ElasticsearchClientUri(elasticUri()))
      //Settings.settingsBuilder.put("client.transport.ping_timeout", "30s").build,
  }
}