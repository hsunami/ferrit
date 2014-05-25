package org.ferrit.core.test

import scala.concurrent.{ExecutionContext, Future}
import org.ferrit.core.http.{Request, Response}
import org.ferrit.core.uri.CrawlUri


/**
 * A FakeHttpClient that simply maps an incoming request URI
 * to a canned Response in a map. If the Response is not found 
 * in the map a 404 not found response is returned.
 */
class ParrotHttpClient(responses: Map[String, PartResponse])(implicit ec: ExecutionContext) extends FakeHttpClient {

  require(responses != null, "A Map of request URI to Response is required")

  override implicit val _ec: ExecutionContext = ec

  override def handleRequest(request: Request):Response = {
    val uri: String = request.crawlUri.crawlableUri
    val pr: PartResponse = responses.getOrElse(uri, FakeHttpClient.NotFound)
    pr.toResponse(request)
  }

}
