package org.ferrit.core.crawler

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.ferrit.core.filter.{UriFilter, FirstMatchUriFilter}
import org.ferrit.core.filter.FirstMatchUriFilter.{Accept, Reject}
import org.ferrit.core.uri.CrawlUri


class TestCrawlConfigValidator extends FlatSpec with ShouldMatchers {
  
  import CrawlConfigValidator.Passed

  def makeConfig(seeds: Seq[String], uriFilter: UriFilter, tests: Option[Seq[String]]) = CrawlConfig(
      id = "1234",
      userAgent = Some("Test Agent"),
      crawlerName = "Test Crawler",
      seeds = seeds.map(s => CrawlUri(s)),
      uriFilter = uriFilter,
      tests = tests,
      obeyRobotRules = true,
      crawlDelayMillis = 0,
      crawlTimeoutMillis = 1,
      maxDepth = 1,
      maxFetches = 1,
      maxQueueSize = 1,
      maxRequestFails = 0
    )


  behavior of "CrawlConfigValidator"

  it should "find that a seed is accepted" in {

    val uri = "http://site.net"
    val config: CrawlConfig = makeConfig(
      Seq(uri), 
      new FirstMatchUriFilter(Seq(Accept(uri.r))),
      Some(Seq(s"should accept: $uri"))
    )

    val results = CrawlConfigValidator.testConfig(config)

    results should equal (CrawlConfigValidatorResults(
        true, 
        Seq(CrawlConfigValidatorResult(CrawlUri(uri), true, Passed)),
        Seq(CrawlConfigValidatorResult(CrawlUri(uri), true, Passed))
    ))
  }

  it should "find that a seed is not accepted" in {

    val uri1 = "http://site.net"
    val uri2 = "http://other.site.net"
    
    val config: CrawlConfig = makeConfig(
      Seq(uri2), 
      new FirstMatchUriFilter(Seq(Accept(uri1.r))),
      Some(Seq("should accept: http://site"))
    )

    val results = CrawlConfigValidator.testConfig(config)

    results.allPassed should equal (false)    
    results.seedResults.size should equal (1)
    results.seedResults(0).passed should equal (false)
  }

  it should "test additional custom accept and reject URIs" in {
    
    val uri1 = "http://site.net"
    val uri2 = "http://site.net/page1"
    val uri3 = "http://other.site.net/page1"
    val uri4 = "http://site.net/page2"
    val uris = Seq(uri1, uri2, uri3, uri4)
    
    def doTest(tests: Seq[String]) = CrawlConfigValidator.testConfig(
      makeConfig(
        Seq(uri2), 
        new FirstMatchUriFilter(Seq(Accept(uri1.r))),
        Some(tests)
      )
    )

    {
      val results = doTest(uris.map(u => s"should accept: $u"))
      results.allPassed should equal (false)
      results.testResults.map(_.passed) should equal (Seq(true, true, false, true))
    }

    {
      val results = doTest(uris.map(u => s"should reject: $u"))
      results.allPassed should equal (false)
      results.testResults.map(_.passed) should equal (Seq(false, false, true, false))
    }

  }

}