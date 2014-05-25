package org.ferrit.core.crawler

import org.joda.time.DateTime
import org.ferrit.core.uri.{CrawlUri, FetchJob}
import org.ferrit.core.model.CrawlJob

sealed abstract class CrawlOutcome(val state: String, val message: String)
object KeepOnTruckin extends CrawlOutcome("Continuing", "Keep crawling")
object CompletedOkay extends CrawlOutcome("Completed", "Completed Okay")
object StopRequested extends CrawlOutcome("Stopped", "Stop requested")
object TooManyQueued extends CrawlOutcome("Aborted", "Too many URIs queued for crawling")
object TooManyFetches extends CrawlOutcome("Aborted", "Crawl exceeded maximum fetch requests")
object TooManyFetchesFailed extends CrawlOutcome("Aborted", "Too many failed fetches")
object CrawlTimeout extends CrawlOutcome("Aborted", "Crawl exceeded maximum crawl time")
case class InternalError(reason: String, t: Throwable) extends CrawlOutcome("Aborted", reason)


/**
 * Crawler aborted messages are typically embedded strings within
 * Actor messages but are not the Actor message themselves.
 * They just make it easier to pattern match expected results in tests.
 */
object CrawlAborted {
  
  val UserAgentMissing = 
    "Please set a sensible userAgent value in your crawler configuration." + 
    "This is used to form a User-agent header during HTTP fetch requests " +
    "to help identify your crawler to the web server. Some web servers will " +
    "block requests if a user agent is not defined. " + 
    "The user agent is also used when evaluating Robots Exclusion rules."

  val SeedsAreMissing = 
    "seed URIs are required to start a crawler"

  val SeedsAreRejected = 
    "The crawler URI filter rejected the seeds: %s"

  val UriFilterMissing = 
    "The crawler URI filter is missing"

}
