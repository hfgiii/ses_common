package org.hfgiii.ses.common.admin

import com.sksamuel.elastic4s.ElasticClient
import org.elasticsearch.common.settings.ImmutableSettings
import com.sksamuel.elastic4s.ElasticDsl._

trait EsAdmin {

  def initLocalEs4sClient(settings:ImmutableSettings.Builder):ElasticClient =
     ElasticClient.local(settings.build)

  def initRemoteEs4sClient(settings:ImmutableSettings.Builder,addresses:(String,Int)*):ElasticClient =
    ElasticClient.remote(settings.build,addresses: _*)

  def shutdownElasticsearch(implicit client:ElasticClient) {
    client.shutdown.await
  }

  def emptyClient:ElasticClient = ElasticClient.local


}

object EsAdmin extends EsAdmin
