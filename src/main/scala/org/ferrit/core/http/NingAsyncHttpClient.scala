package org.ferrit.core.http

import scala.concurrent.Future
import com.ning.http.client.{AsyncHttpClient, AsyncHandler}
import com.ning.http.client.AsyncCompletionHandler
import com.ning.http.client.AsyncHandler._
import org.ferrit.core.http.{Response => OurResponse}
import com.ning.http.client.{Response => NingResponse}
import com.ning.http.client._
import scala.concurrent.Promise
import java.net.URI
import java.util.concurrent.{Future => JFuture}
import java.util.concurrent.Executor
import java.io.ByteArrayOutputStream
import java.util.{Map => JMap, List => JList, Set => JSet}
import scala.collection.JavaConverters._

/**
 * HttpClient implementation that uses the Ning AsyncHttpClient.
 */
class NingAsyncHttpClient(config: HttpClientConfig) extends HttpClient {

  private val client = {
    val conf = new AsyncHttpClientConfig.Builder()
      .setAllowPoolingConnection(config.useConnectionPooling)
      .setRequestTimeoutInMs(config.requestTimeout)
      .setFollowRedirects(config.followRedirects)
      .setCompressionEnabled(config.useCompression)
      .build()
    new AsyncHttpClient(conf)
  }

  override def shutdown(): Unit = client.close()

  override def request(request: Request): Future[OurResponse] = {
    
    val allHeaders = request.headers ++ Map[String, String](
      "User-Agent" -> request.userAgent,
      "Pragma" -> "no-cache",
      "Cache-Control" -> "no-cache",
      "Connection" -> "keep-alive",
      "Keep-Alive" -> s"${config.keepAlive}"
    )

    val promise = Promise[OurResponse]()
    val handler = createAsyncHandler(request, promise)
    val reqBuilder = client.prepareGet(request.crawlUri.crawlableUri)

    allHeaders.foreach(pair => reqBuilder.addHeader(pair._1, pair._2))
    reqBuilder.execute(handler)
    promise.future

  }

  def createAsyncHandler(request: Request, promise:Promise[OurResponse]):AsyncHandler[OurResponse] = {
    //new CompletionHandler(request, promise) // for access to Ning Response
    new Handler(request, promise) // simpler, but no access to Ning Response
  }
  
  /**
   * By extending AsyncCompletionHandler instead of AsyncHandler we
   * get access to the Ning Response in onCompleted but the tradeoff is
   * having to override many other methods and calling super, not sure what
   * extra (and possible unneccesary work is being done there.)
   */
  // class CompletionHandler(request: Request, promise:Promise[OurResponse]) extends AsyncCompletionHandler[OurResponse] {

  //   var bytes = new ByteArrayOutputStream
  //   var headerMap: Map[String, List[String]] = Map.empty
  //   var statusCode: Int = -1
  //   var length = 0

  //   var timeStatus: Long = _
  //   var timeHeaders: Long = _
  //   var timeCompleted: Long = _

  //   def now():Long = System.currentTimeMillis
  //   var start = now

  //   override def onStatusReceived(status: HttpResponseStatus):STATE = {
  //     super.onStatusReceived(status)
  //     statusCode = status.getStatusCode()
  //     timeStatus = now() - start
  //     STATE.CONTINUE // what if status code > = 500?
  //   }

  //   override def onHeadersReceived(h: HttpResponseHeaders):STATE = {
  //     super.onHeadersReceived(h)        
  //     // headers is a FluentCaseInsensitiveStringsMap
  //     val set: JSet[JMap.Entry[String, JList[String]]] = h.getHeaders().entrySet()
  //     set.asScala.foreach(e => 
  //       headerMap += (e.getKey() -> e.getValue().asScala.toList)
  //     )
  //     timeHeaders = now() - start
  //     STATE.CONTINUE
  //   }

  //   override def onBodyPartReceived(bodyPart: HttpResponseBodyPart): STATE = {
  //     super.onBodyPartReceived(bodyPart)
  //     val b: Array[Byte] = bodyPart.getBodyPartBytes()
  //     length += b.length
  //     bytes.write(b)
  //     if (maxSizeExceeded) STATE.ABORT else STATE.CONTINUE
  //   }

  //   def onCompleted(ningResponse: NingResponse):OurResponse = {
      
  //     // TODO 
  //     // If this URI is different to the one in the Request, 
  //     // we need to report the difference in the Response.
  //     // Unfortunately ... we don't know if the URI is different because of a redirect
  //     // or simply that the Java URI is different to the one provided in the request?
  //     val uri:URI = ningResponse.getUri()


  //     if (maxSizeExceeded) 
  //       throw new Exception(s"Max content size exceeded: ${config.maxContentSize}")
  //     else {
  //       timeCompleted = now() - start
  //       val response =
  //         DefaultResponse(
  //           statusCode,
  //           headerMap,
  //           bytes.toByteArray(),
  //           Stats(timeStatus, timeHeaders, timeCompleted),
  //           request
  //         )
  //       promise.success(response)
  //       response // only needed for the Ning AsyncHandler, not the Promise
  //     }
  //   }

  //   def maxSizeExceeded = length > config.maxContentSize

  //   override def onThrowable(t: Throwable):Unit = {
  //     super.onThrowable(t)
  //     promise.failure(t)
  //   }

  // }

  /**
   * Implements AsyncDirectly but we don't get the Ning Response in onCompleted()
   */
  class Handler(request: Request, promise:Promise[OurResponse]) extends AsyncHandler[OurResponse] {

    var bytes = new ByteArrayOutputStream
    var headerMap: Map[String, List[String]] = Map.empty
    var statusCode: Int = -1
    var length = 0

    var timeStatus: Long = _
    var timeHeaders: Long = _
    var timeCompleted: Long = _

    def now():Long = System.currentTimeMillis
    var start = now

    def onStatusReceived(status: HttpResponseStatus):STATE = {
      statusCode = status.getStatusCode()
      timeStatus = now() - start
      STATE.CONTINUE // what if status code > = 500?
    }

    def onHeadersReceived(h: HttpResponseHeaders):STATE = {
      // headers is a FluentCaseInsensitiveStringsMap
      val set: JSet[JMap.Entry[String, JList[String]]] = h.getHeaders().entrySet()
      set.asScala.foreach(e => 
        headerMap += (e.getKey() -> e.getValue().asScala.toList)
      )
      timeHeaders = now() - start
      STATE.CONTINUE
    }

    def onBodyPartReceived(bodyPart: HttpResponseBodyPart): STATE = {
      val b: Array[Byte] = bodyPart.getBodyPartBytes()
      length += b.length
      bytes.write(b)
      if (maxSizeExceeded) STATE.ABORT else STATE.CONTINUE
    }

    def onCompleted():OurResponse = {
      if (maxSizeExceeded) 
        throw new Exception(s"Max content size exceeded: ${config.maxContentSize}")
      else {
        timeCompleted = now() - start
        val response =
          DefaultResponse(
            statusCode,
            headerMap,
            bytes.toByteArray(),
            Stats(timeStatus, timeHeaders, timeCompleted),
            request
          )
        promise.success(response)
        response // only needed for the Ning AsyncHandler, not the Promise
      }
    }

    def maxSizeExceeded = length > config.maxContentSize

    def onThrowable(t: Throwable):Unit = promise.failure(t)

  }

}