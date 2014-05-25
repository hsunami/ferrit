package org.ferrit.core.crawler

import org.ferrit.core.uri.CrawlUri
import org.ferrit.core.util.KeyValueParser


/**
 * A utility to aid crawler configuration in a user interface.
 * Runs a test on a CrawlConfig, more specifically testing the seed URIs
 * and additional test case URIs against the URI filter rules.
 */
object CrawlConfigValidator {

  val testDirectives = Seq("should accept", "should reject")
  val Passed = "passed"

  def testConfig(config: CrawlConfig):CrawlConfigValidatorResults = {

    def testUri(uri: CrawlUri, shouldAccept: Boolean) = {
      val f = config.uriFilter
      val passed = if (shouldAccept) f.accept(uri) else !f.accept(uri)
      val msg = if (passed) Passed else f.explain(uri)
      CrawlConfigValidatorResult(uri, passed, msg)
    }

    val sresults = config.seeds.map(testUri(_, true))
    
    val tresults = config.tests match {
      case None => Nil
      case Some(tests) =>
        KeyValueParser.parse(testDirectives, tests, 
        {(key:String, value:String) => key match {
            case "should accept" => testUri(CrawlUri(value), true)
            case "should reject" => testUri(CrawlUri(value), false)
          }
        }
      )
    }
    
    val allPassed = (sresults ++ tresults).forall(_.passed)
    
    CrawlConfigValidatorResults(allPassed, sresults, tresults)
  }

}

case class CrawlConfigValidatorResults(
  allPassed: Boolean, 
  seedResults: Seq[CrawlConfigValidatorResult],
  testResults: Seq[CrawlConfigValidatorResult]
)

case class CrawlConfigValidatorResult(
  uri: CrawlUri, 
  passed: Boolean, 
  message: String
)
