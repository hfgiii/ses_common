package org.hfgiii.ses.common.admin

import com.sksamuel.elastic4s.ElasticClient
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.node.NodeBuilder._
import com.sksamuel.elastic4s.ElasticDsl._

trait EsAdmin {

  def initLocalEs4sClient(settings:ImmutableSettings.Builder):ElasticClient =
     ElasticClient.local(settings.build)


  def initLocalEs4sClient:ElasticClient = {
    val node = nodeBuilder.data(false).local(false).node

    ElasticClient.fromNode(node)
  }


  def initRemoteEs4sClient(settings:ImmutableSettings.Builder,addresses:(String,Int)*):ElasticClient =
    ElasticClient.remote(settings.build,addresses: _*)


  def initRemoteEs4sClient(addresses:(String,Int)*):ElasticClient =
    ElasticClient.remote(addresses: _*)


  def shutdownElasticsearch(implicit client:ElasticClient) =
    client.shutdown.await

  def closeElasticsearchClient(implicit client:ElasticClient) =
      client.close

  def emptyClient:ElasticClient = ElasticClient.local

  def withES(ex: => Unit)(implicit remote:Boolean,client:ElasticClient): Unit =
    withES(remote)(ex)


  def withES(remote:Boolean,shutdown:Boolean=false)(ex: => Unit)(implicit client:ElasticClient): Unit = {

    ex

    if(shutdown) {
      if (remote)
        closeElasticsearchClient
      else
        shutdownElasticsearch
    }
  }

}

object EsAdmin extends EsAdmin
