## ses_common

The __ses_common__ project provides tools for scala [elasticearch](http://www.elasticsearch.org/) clients; in particular, [elastic4s](https://github.com/sksamuel/elastic4s), which provides a scala DSL for elasticsearch operations . The current version provides tools to read and transform the contents of the eleasticsearch Java API search response classes: [SearchResponse](https://github.com/elasticsearch/elasticsearch/blob/master/src/main/java/org/elasticsearch/action/search/SearchResponse.java) and [InternalSearchResponse](https://github.com/elasticsearch/elasticsearch/blob/master/src/main/java/org/elasticsearch/search/internal/InternalSearchResponse.java).

Given a SearchResponse, the ses_common function [hitsFromSearchResponse](https://github.com/hfgiii/ses_common/blob/master/core/src/main/scala/org/hfgiii/ses/common/dsl/response/readers/ResponseReaderDsl.scala) injects the SearchResponse [SearchHit](https://github.com/elasticsearch/elasticsearch/blob/master/src/main/java/org/elasticsearch/search/SearchHit.java) list into a [PartialFunction](http://www.scala-lang.org/api/current/index.html#scala.PartialFunction) passed to _hitsFromSearchResponse_. Hit source and hit fields can be read from individual SearchHit instances and processed in the PartialFunction with the [fromHit and fromHitFields](https://github.com/hfgiii/ses_common/blob/master/core/src/main/scala/org/hfgiii/ses/common/dsl/response/readers/ResponseReaderDsl.scala), respectively.

The following code snippet shows the use of these functions:

``` scala

      hitsFromSearchResponse {
        client.execute {  // (1)
          search in "sesportfolio" types "positions" query matchall size 256 sort {
            by field "date" order SortOrder.ASC
          } scriptfields (
              script field "balance" script "portfolioscript" lang "native" 
              script field "date" script "doc['date'].value" lang "groovy"
            )
        }
      }{ // (2)
        case hits:SearchHits =>
          val formatter = new SimpleDateFormat("yyyy-MM-dd")

          Option (hits.getHits.foldLeft(RoRSimpleIndexAccumulator(lastClose = 1000000d)) {
          (ror,hit) =>
          
            // (3)
            val PortfolioBalance(date,balance) = fromHitFields[PortfolioBalance](hit)
            
            val ts =  new Date(date)
            
            val rorCalc = 
             elif(ror.lastClose == 0d || ror.lastClose == balance) 0d
             else (balance / ror.lastClose) - 1
                          
            val idxDef = 
            index into "simulation/ror" fields (
              "date" -> formatter.format(ts),
              "rate_of_return" -> rorCalc
              )

            RoRSimpleIndexAccumulator(balance, idxDef :: ror.rorIndexDefinitions)
        })
      }
```

Legend :

    1. This is an elasticsearch search query written with the elastic4s scala DSL.
    2. 'hitsFromSearchResponse' injects the SearchHits list into this PartialFunction.
    3. The 'fromHitFields' function transforms the Map representation of the SearchHit fields into the PortfolioBalance case class.
    
#### Using ses_common

Cyrrently, __ses_common__ is not published to public repository. To use __ses_common__ in your appliction you must first clone the __ses_common__ project locally and run the following __sbt__ command in the resultant directory:

 ```
     sbt publish-local
 ```
 
 In the __sbt__ depedencies section in __build.sbt__ or __Build.scala__ add the following:
 
 ``` scala
    
    libraryDepencies ++= (
    "org.hfgiii.ses.common" %% "ses_common_macros" % "0.1.0-SNAPSHOT",
    "org.hfgiii.ses.common" %% "ses_common" % "0.1.0-SNAPSHOT",)
 
 ```   