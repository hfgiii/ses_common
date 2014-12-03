package org.hfgiii.ses.common.load

import com.sksamuel.elastic4s.{IndexDefinition, ElasticClient}
import com.sksamuel.elastic4s.ElasticDsl._
import org.elasticsearch.indices.IndexMissingException


trait EsIndexLoad {

  def blockUntilCount(expected: Long,
                      index: String,
                      types: String*)(implicit client:ElasticClient) {

    var backoff = 0
    var actual = 0l

    while (backoff <= 50000 && actual != expected) {
      if (backoff > 0)
        Thread.sleep(100)
      backoff = backoff + 1
      try {
        actual = client.execute {
          count from index types types
        }.await.getCount
      } catch {
        case e: IndexMissingException => 0
      }
    }
    println(s"actual is $actual")
    require(expected == actual, s"Block failed waiting on count: Expected was $expected but actual was $actual")
  }

  def bulkIndexLoad(index:String,indexDefs:Seq[IndexDefinition],accumDocs:Long)(implicit client:ElasticClient) {

    client execute {
      bulk(indexDefs: _ *)
    }

    blockUntilCount(accumDocs,index)

  }

}

object EsIndexLoad extends EsIndexLoad
