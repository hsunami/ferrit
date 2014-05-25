package org.ferrit.core.crawler

import org.ferrit.core.util.Counters

/**
 * A strategy to work out if a crawler should continue crawling or not.
 * Before each fetch a crawler will query the strategy to find out 
 * if it should continue crawling or stop.
 * This strategy returns a CrawlOutcome decision. 
 *
 * A special outcome called KeepCrawling instructs the crawler to keep going. 
 * Any other outcome requires the crawler to stop.
 */
trait GameOver {

  def query(
    config: CrawlConfig, 
    status: CrawlStatus, 
    counters: Counters, 
    fetchesPending: Int):CrawlOutcome
  
}