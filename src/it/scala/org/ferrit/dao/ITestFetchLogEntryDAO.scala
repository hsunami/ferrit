package org.ferrit.dao

import scala.util.Random
import org.joda.time.DateTime
import org.scalatest.matchers.ShouldMatchers
import org.ferrit.core.model.FetchLogEntry


class ITestFetchLogEntryDAO extends AbstractDAOTest with ShouldMatchers {
  
  val fleDao = daoFactory.fetchLogEntryDao

  behavior of "FetchLogEntryDAO"

  it should "insert a new row" in {

    val crawlerId = makeStringId
    val jobId = makeStringId
  
    val fle = FetchLogEntry(
      crawlerId, 
      jobId, 
      new DateTime, 
      "http://site.net",
      0,
      200,
      Some("text/html"),
      1,2,3,4,5,6,7,8
    )

    fleDao.insert(fle)
    fleDao.find(jobId) match {
      case Nil => fail("not found")
      case Seq(fle2, _*) => fle should equal (fle2)
    }

  }

}