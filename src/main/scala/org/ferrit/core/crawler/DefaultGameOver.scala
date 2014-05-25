package org.ferrit.core.crawler

import org.ferrit.core.util.Counters

/**
 * A basic implementation of the GameOver.
 */
class DefaultGameOver extends GameOver {
  
  import CrawlWorker._

  /** 
   * Allow a certain number of fails through before other considerations
   * otherwise a crawler with only 1 seed and a request fail
   * on the second page will abort too early.
   */
  val MinFailCount = 5

  
  override def query(
    c: CrawlConfig, 
    status: CrawlStatus, 
    ct: Counters, 
    fetchesPending: Int):CrawlOutcome = {
    
    if (status.shouldStop) StopRequested
    else if (fetchesPending == 0) CompletedOkay
    else if (fetchesPending >= c.maxQueueSize) TooManyQueued
    else if (ct.get(FetchSucceeds) >= c.maxFetches) TooManyFetches
    else if (status.now.isAfter(status.crawlStop)) CrawlTimeout
    else if (isFailedCrawl(
              c.seeds.size, 
              c.maxRequestFails,
              ct.get(FetchAttempts),
              ct.get(FetchFails))) TooManyFetchesFailed

    else KeepOnTruckin
  }

  /**
   * Simple algorithm to figure out if crawler is failing too much 
   * and should stop. Factored out to be more testable, at the
   * expense of boilerplate method signature.
   *
   * A fail percentage is calculated and if it exceeds the user-defined fail 
   * percentage threshold then the crawl is considered failed.
   *
   * The percentage test is bypassed until all the seed URIs have been 
   * actioned as fetch requests.
   *
   */
  private [crawler] def isFailedCrawl(
      totalSeeds: Int, 
      maxFailPercent: Double, 
      fetchAttempts: Int, 
      fetchFails: Int): Boolean = {

    if (fetchAttempts <= totalSeeds || fetchAttempts < MinFailCount) {
      false
    } else {
      val failPercent = fetchFails.toDouble / fetchAttempts.toDouble
      failPercent >= maxFailPercent
    }

  }

}