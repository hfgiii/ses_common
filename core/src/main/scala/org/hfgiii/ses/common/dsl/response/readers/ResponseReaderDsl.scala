package org.hfgiii.ses.common.dsl.response.readers


import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.search.{SearchHits, SearchHit}
import org.elasticsearch.search.aggregations.Aggregation
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation.Bucket
import com.sksamuel.elastic4s.ElasticDsl._
import shapeless._
import syntax.typeable._

import scala.collection.JavaConversions._
import scala.concurrent.Future
import org.hfgiii.ses.common.macros.SesMacros._

trait ResponseReaderDsl {

  protected def idMapper(key:String) = key

  def toMap[T : Mappable](t:T,keyMapper:String => String) =
    implicitly[Mappable[T]].toMap(t, keyMapper)

  def fromMap[T : Mappable](map: Map[String, Any],keyMapper:String => String) =
    implicitly[Mappable[T]].fromMap(map,keyMapper:String => String)

  def fromHit[C : Mappable](hit:SearchHit):Option[C] =
    hit.sourceAsMap.cast[Map[String,Any]].map {
      m => fromMap(m,idMapper)
    }

  def fromHitFields[C : Mappable](hit:SearchHit):C = {
    val hitFields =  hit.getFields
    val m = hitFields.keySet.foldLeft(Map.empty[String,Any]) {
      (m,key) => m + (key -> hitFields.get(key).getValue)
    }
    fromMap(m,idMapper)
  }

  def aggActionFromBucket(bucket:Option[Bucket])(aggName:String)(action :PartialFunction[Aggregation,Unit]) =
    bucket.fold(println(s"No $aggName")) { sp =>
      Option(sp.getAggregations.asMap.get(aggName)).fold(println(s"No $aggName")) {
        action orElse {
          case x: Aggregation =>
            println(s"Unexpected aggregation returned: ${x.getClass.getCanonicalName}")
        }
      }
    }

  def bucketFromSearchResponse(sp:Future[SearchResponse])(aggName:String)(action :PartialFunction[Aggregation,Option[Bucket]]):Option[Bucket] =
    bucketFromSearchResponse(sp.await)(aggName)(action)

  def bucketFromSearchResponse(sp:SearchResponse)(aggName:String)(action :PartialFunction[Aggregation,Option[Bucket]]):Option[Bucket] =
    Option(sp.getAggregations.asMap.get(aggName)) match {
      case Some(agg) => action.applyOrElse (agg, (x:Aggregation) => {
        println(s"Unexpected aggregation returned: ${x.getClass.getCanonicalName}"); None

      })

      case None => println(s"No $aggName") ; None
    }

  def aggActionFromSearchResponse(sp:Future[SearchResponse])(aggName:String)(action :PartialFunction[Aggregation,Unit]):Unit =
    aggActionFromSearchResponse(sp.await)(aggName)(action)

  def aggActionFromSearchResponse(sp:SearchResponse)(aggName:String)(action :PartialFunction[Aggregation,Unit]):Unit = {
    Option(sp.getAggregations.asMap.get(aggName)).fold (println(s"No $aggName")) {
      action orElse {
        case x:Aggregation =>
          println(s"Unexpected aggregation returned: ${x.getClass.getCanonicalName}")

      }
    }
  }

  def aggFromSearchResponse[R](sp:Future[SearchResponse])(aggName:String)(action :PartialFunction[Aggregation,Option[R]]):Option[R] =
    aggFromSearchResponse(sp.await)(aggName)(action)

  def aggFromSearchResponse[R](sp:SearchResponse)(aggName:String)(action :PartialFunction[Aggregation,Option[R]]):Option[R] =
    Option(sp.getAggregations.asMap.get(aggName)) match {
      case Some(agg) => action.applyOrElse (agg, (x:Aggregation) => {
        println(s"Unexpected aggregation returned: ${x.getClass.getCanonicalName}"); None

      })

      case None => println(s"No $aggName") ; None
    }


  def hitsFromSearchResponse[R](sp:Future[SearchResponse])(action :PartialFunction[SearchHits,Option[R]]):Option[R] =
    hitsFromSearchResponse(sp.await)(action)


  def hitsFromSearchResponse[R](sp:SearchResponse)(action :PartialFunction[SearchHits,Option[R]]):Option[R] =
    Option(sp.getHits) match {
      case Some(hits) => action.applyOrElse(hits, (x:SearchHits) => {
        println(s"Unexpected hits returned: ${x.getClass.getCanonicalName}"); None

      })

      case None => println(s"No Search Hits!") ; None

    }

}

object ResponseReaderDsl extends ResponseReaderDsl

