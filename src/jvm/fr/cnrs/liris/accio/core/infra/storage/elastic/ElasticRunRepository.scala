/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016 Vincent Primault <vincent.primault@liris.cnrs.fr>
 *
 * Accio is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Accio is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Accio.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.cnrs.liris.accio.core.infra.storage.elastic

import com.twitter.finatra.json.FinatraObjectMapper
import fr.cnrs.liris.accio.core.domain._
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.QueryBuilders._
import org.elasticsearch.search.sort.SortOrder

class ElasticRunRepository(mapper: FinatraObjectMapper, client: Client, prefix: String) extends RunRepository {
  override def find(query: RunQuery): RunList = {
    val qb = boolQuery()
    query.cluster.foreach(cluster => qb.filter(termQuery("cluster", cluster)))
    query.environment.foreach(environment => qb.filter(termQuery("environment", environment)))
    query.owner.foreach(owner => qb.filter(termQuery("owner.name", owner)))
    query.status.foreach(status => qb.filter(termQuery("state.status", status)))
    query.workflow.foreach(workflowId => qb.filter(termQuery("pkg.workflowId", workflowId)))
    query.name.foreach(name => qb.must(matchQuery("name", name)))

    val searchQuery = client.prepareSearch(runsIndex)
      .setTypes(runsType)
      .setQuery(qb)
      .addSort("createdAt", SortOrder.DESC)
    query.limit.foreach(searchQuery.setSize)
    query.offset.foreach(searchQuery.setFrom)

    val resp = searchQuery.get()
    val results = resp.getHits.hits.toSeq.map(hit => mapper.parse[Run](hit.getSourceAsString))
    RunList(results, resp.getHits.totalHits.toInt)
  }

  override def find(query: LogsQuery): Seq[RunLog] = {
    //.setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
    val qb = boolQuery()
      .filter(termQuery("nodeName", query.nodeName))
      .filter(termQuery("runId", query.runId.value))
    query.classifier.foreach(classifier => qb.filter(termQuery("classifier", classifier)))
    query.since.foreach(since => qb.filter(rangeQuery("createdAt").from(since)))

    val searchQuery = client.prepareSearch(logsIndex)
      .setTypes(logsType)
      .setQuery(qb)
      .addSort("createdAt", SortOrder.DESC)
    query.limit.foreach(searchQuery.setSize)

    val resp = searchQuery.get()
    resp.getHits.hits.toSeq.map(hit => mapper.parse[RunLog](hit.getSourceAsString))
  }

  override def save(run: Run): Unit = {
    val json = mapper.writeValueAsString(run)
    client.prepareIndex(runsIndex, runsType, run.id.value)
      .setSource(json)
      .get()
  }

  override def save(logs: Seq[RunLog]): Unit = {
    val bulkRequest = client.prepareBulk()
    logs.foreach { log =>
      val json = mapper.writeValueAsString(log)
      bulkRequest.add(client.prepareIndex(logsIndex, logsType).setSource(json))
    }
    bulkRequest.get()
  }

  override def get(id: RunId): Option[Run] = {
    val resp = client.prepareGet(runsIndex, runsType, id.value).get
    //TODO: handle not found document.
    Some(mapper.parse[Run](resp.getSourceAsBytes))
  }

  override def exists(id: RunId): Boolean = {
    //TODO
    true
  }

  override def delete(id: RunId): Unit = {
    client.prepareDelete(s"$prefix/runs", "default", id.value).get()
  }

  private def runsIndex = s"${prefix}__runs"

  private def logsIndex = s"${prefix}__logs"

  private def runsType = "default"

  private def logsType = "default"
}
