package org.ferrit.core.filter

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.ferrit.core.filter.PriorityRejectUriFilter._


class TestPriorityRejectUriFilter extends FlatSpec with ShouldMatchers {

  import UriFilter.implicitConvertStringToCrawlUri


  behavior of "PriorityRejectUriFilter"


  it should "reject on empty rules to prevent unbounded crawls" in {
    val um = new PriorityRejectUriFilter(Seq())
    um.accept("http://website1.com/") should equal (false)
    um.accept("http://sub.website1.com/") should equal (false)
  }

  it should "check for accepts but reject even when not explicitly rejected" in {
    val um = new PriorityRejectUriFilter(Seq(
      Accept("http://website1.com/".r)
    ))
    um.accept("http://website1.com/") should equal (true)
    um.accept("http://website2.com/") should equal (false)
    um.accept("http://sub.website1.com/") should equal (false)
  }

  it should "reject when rejected or not explictly accepted" in {
    
    val um = new PriorityRejectUriFilter(
      Seq(Reject("http://website1.com/".r))
    )

    um.accept("http://website1.com/") should equal (false)
    um.accept("http://website2.com/") should equal (false)
    um.accept("http://sub.website1.com/") should equal (false)
  }

  it should "reject over accept in trailing slash edge case" in {
    val um = new PriorityRejectUriFilter(Seq(
      Accept("http://website1.com/".r), 
      Reject("http://website1.com".r)
    ))
    um.accept("http://website1.com/") should equal (false)
    um.accept("http://website1.com") should equal (false)
  }

  it should "accept over reject in trailing slash edge case" in {
    val um = new PriorityRejectUriFilter(Seq(
      Accept("http://website1.com".r), 
      Reject("http://website1.com/".r)
    ))
    um.accept("http://website1.com") should equal (true)
    um.accept("http://website1.com/") should equal (false)
  }

  it should "allow certain accepts" in {
    val um = new PriorityRejectUriFilter(Seq(
      Accept("http://website1.com/".r),
      Accept("http://sub.website1.com/".r)
    ))
    um.accept("http://website1.com/") should equal (true)
    um.accept("http://website1.com/page1") should equal (true)
    um.accept("http://website2.com/") should equal (false)
    um.accept("http://sub.website1.com/") should equal (true)
  }

  it should "specific reject rule overrides general accept rule" in {
    val um = new PriorityRejectUriFilter(Seq(
      Accept("http://website1.com".r),
      Reject("http://website1.com/page2".r)
    ))
    um.accept("http://website1.com/") should equal (true)
    um.accept("http://website1.com/page1") should equal (true)
    um.accept("http://website1.com/page2") should equal (false)
    um.accept("http://website1.com/page2etc") should equal (false)
  }

  it should "reject when there are identical accepts and rejects" in {
    val um = new PriorityRejectUriFilter(Seq(
      Accept("http://website1.com/".r),
      Accept("http://website2.com/".r),
      Reject("http://website1.com/".r)
    ))
    um.accept("http://website1.com/") should equal (false)
    um.accept("http://website1.com/page1") should equal (false)
    um.accept("http://website2.com/") should equal (true)
  }

  it should "only allow matches by prefix" in {
    
    val socialMediaUri = "https://social.media.site.com?uri=http://site.net/page1"

    val uf = new PriorityRejectUriFilter(Seq(Accept("http://site.net/".r)))
    uf.accept("http://site.net/page1") should equal (true)
    uf.accept(socialMediaUri) should equal (false)

    // Unlikely this would be wanted, but strictly speaking it should match
    val uf2 = new PriorityRejectUriFilter(Seq(Accept(".*http://site.net/".r)))
    uf2.accept(socialMediaUri) should equal (true)

  }

  /** 
   * I'm not sure it is possible with this strategy to have a general 
   * reject whilst permitting a specific accept.
   */
  // it should "specific accept rule overrides general reject rule" in {
  //   val um = new PriorityRejectUriFilter(Seq(
  //       Accept("http://website1.com/page2"),
  //       Reject("http://website1.com")
  //   ))
  //   um.accept("http://website1.com/") should equal (false)
  //   um.accept("http://website1.com/page1") should equal (false)
  //   um.accept("http://website1.com/page2") should equal (true)
  //   um.accept("http://website1.com/page2etc") should equal (true)
  // }

}