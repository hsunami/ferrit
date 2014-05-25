package org.ferrit.core.model

import org.ferrit.core.crawler.CrawlConfig

case class Crawler(
  crawlerId: String, 
  config: CrawlConfig
)