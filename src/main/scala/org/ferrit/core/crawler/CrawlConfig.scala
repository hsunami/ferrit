package org.ferrit.core.crawler

import org.ferrit.core.filter.UriFilter
import org.ferrit.core.uri.CrawlUri
import org.ferrit.core.crawler.CrawlAborted._


case class CrawlConfig (

  /**
   * The unique identifier of the crawler configuration.
   */
  id: String,

  /**
   * The User-Agent header sent to web servers during a fetch to identify
   * the crawler. This must set this before starting a crawl.
   */
  userAgent: Option[String],
  
  /**
   * The seeds are the starting points of a crawl and should
   * be ordinary URLs not regex patterns. Additional 
   * follow patterns are needed to accept the seeds for following.
   * Adding seeds by themselves are not enough.
   */
  seeds: Seq[CrawlUri],

  /**
   * Configuration for the UriFilter used by the crawler.
   */
  uriFilter: UriFilter,

  /**
   * One or more test cases to validate the UriFilter rules.
   */
  tests: Option[Seq[String]],

  /**
   * The name of the crawl used for identification in crawl logs and job monitoring tools.
   * For example, the name of the website being crawled or meaningful group
   * name when crawling multiple sites.
   */
  crawlerName: String,
  
  /**
   * A custom politeness setting to use if the 'Crawl-delay' directive is
   * not found in the robots.txt file.
   */
  crawlDelayMillis: Int,
  
  /**
   * The length of time a crawl can run before quitting.
   */
  crawlTimeoutMillis: Long,

  /**
   * Set the maximum depth of the crawl, which is to say the number of
   * links away from the seed URIs. A depth of 0 means that only the 
   * seed URIs will be crawled.
   */
  maxDepth: Int,

  /**
   * The maximum number of fetches that the crawler can make during the crawl. 
   */
  maxFetches: Int,
  
  /**
   * The maximum number of URLs that can be queued for fetching.
   * The crawler will quit if the number of queued URLs exceeds this value.
   * This is to prevent out-of-memory exceptions and alert you about larger than
   * expected queue sizes. 
   */
  maxQueueSize: Int,
  
  /**
   * The % percentage of request fails tolerated before the crawler gives up.
   * This prevents misconfigured crawlers from running on indefinitely.
   * Too many 404 errors could indicate that the seed URIs are incorrect.
   * Too many 500 errors could indicate an over-eager crawler or a server
   * trying to ban the crawler.
   */
  maxRequestFails: Double

) {
  
  def getUserAgent = userAgent match {
    case Some(ua) if ua != null && ua.trim.nonEmpty => ua
    case _ => failBecause(UserAgentMissing)
  }

  def validate:Unit = {
          
    getUserAgent

    if (uriFilter == null) {
      failBecause(UriFilterMissing)
    }

    if (seeds == null || seeds.isEmpty) {
      failBecause(SeedsAreMissing)
    }

    if (!seeds.forall(uriFilter.accept)) {
      val advice = seeds.map(uriFilter.explain).mkString(" and ")
      failBecause(SeedsAreRejected.format(advice))
    }

  }

  private def failBecause(msg: String) = throw new CrawlRejectException(msg)

}