package org.ferrit.core.crawler

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers


class TestDefaultGameOver extends FlatSpec with ShouldMatchers {
  
  behavior of "DefaultGameOver"

  val gameOver = new DefaultGameOver

  it should "not fail until seeds are converted to URI fetches" in {
    
    gameOver.isFailedCrawl(
          totalSeeds = 1,
          maxFailPercent = 0.1,
          fetchAttempts = 1,
          fetchFails = 0) should equal(false)

    gameOver.isFailedCrawl(
          totalSeeds = 10,
          maxFailPercent = 0.1,
          fetchAttempts = 10,
          fetchFails = 10) should equal(false)

 }

 it should "not fail until threshold reached" in {
    
    gameOver.isFailedCrawl(
          totalSeeds = 1,
          maxFailPercent = 0.6,
          fetchAttempts = 2,
          fetchFails = 1) should equal(false)

    gameOver.isFailedCrawl(
          totalSeeds = 1,
          maxFailPercent = 0.5,
          fetchAttempts = 10,
          fetchFails = 4) should equal(false)

 }

 it should "fail if there are too many failed fetches" in {
    
    gameOver.isFailedCrawl(
          totalSeeds = 1,
          maxFailPercent = 0.1,
          fetchAttempts = 1000,
          fetchFails = 100) should equal(true)

    gameOver.isFailedCrawl(
          totalSeeds = 1,
          maxFailPercent = 0.2,
          fetchAttempts = 1000,
          fetchFails = 200) should equal(true)

    gameOver.isFailedCrawl(
          totalSeeds = 1,
          maxFailPercent = 0.2,
          fetchAttempts = 10000,
          fetchFails = 2000) should equal(true)

  }

}