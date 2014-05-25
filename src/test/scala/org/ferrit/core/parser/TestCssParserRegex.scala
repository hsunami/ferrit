package org.ferrit.core.parser

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.mockito.Mockito._
import org.ferrit.core.http.{Request, Response, Get}
import org.ferrit.core.uri.CrawlUri
import org.ferrit.core.util.HttpUtil._
import org.ferrit.core.util.TagUtil
import org.ferrit.core.test.FakeHttpClient.CssResponse


class TestCssParserRegex extends FlatSpec with ShouldMatchers {
  
  val css_url = TagUtil.CssTagEquiv // alias for readability

  behavior of "CssParserRegex"


  it should "handle just the CSS media type" in {
    
    def responseOf(contentType: String):Response = {
      val r = mock(classOf[Response])
      when (r.contentType) thenReturn Some(contentType)
      r
    }

    val parser = new CssParserRegex
    parser.canParse(responseOf("text/css")) should equal (true)
    parser.canParse(responseOf("text/html")) should equal (false)
    parser.canParse(responseOf("html")) should equal (false)
    parser.canParse(responseOf("text")) should equal (false)
    parser.canParse(responseOf("")) should equal (false)
    
    intercept[ParseException] { parser.parse(responseOf("text/html")) }
    intercept[ParseException] { parser.parse(responseOf("")) }

  }

  it should "parse links in a CSS file ignoring comments" in {
    
    val parser = new CssParserRegex
    val request = Get("", CrawlUri("http://site.com/screen.css"))
    val cssFile = """
      |#content {
      |  color: red;
      |  background: url('image1.png');
      |}
      |#content2 {
      |  color: blue;
      |  background: url("image2.png");
      |}
      |/* background: url('imagexx.png'); */
      |#content3 {
      |  border: 1px solid #999999;
      |  background: url(image3.png);
      |}
      |#content4 {
      |  font-family: Comical;
      |  background: url ( "image4.png");
      |}
      |/* 
      |  background: url('imagex.png');
      |*/
      """.stripMargin

    val response = CssResponse(cssFile).toResponse(request)
    val result:ParserResult = parser.parse(response)
      
    result.links should contain(Link(css_url, "image1.png", "", false, Some(CrawlUri("http://site.com/image1.png")), None))
    result.links should contain(Link(css_url, "image2.png", "", false, Some(CrawlUri("http://site.com/image2.png")), None))
    result.links should contain(Link(css_url, "image3.png", "", false, Some(CrawlUri("http://site.com/image3.png")), None))
    result.links should contain(Link(css_url, "image4.png", "", false, Some(CrawlUri("http://site.com/image4.png")), None))
    result.links.size should equal (4)

  }

  it should "handle URI parse failures" in {
    
    val parser = new CssParserRegex
    val request = Get("", CrawlUri("http://site.com/screen.css"))
    val cssFile = """
      |#content {
      |  background: url('image1.png?id');
      |}
      |#content2 {
      |  background: url('image2.png?id==='); /* <------- bad URI */
      |}
      |#content3 {
      |  background: url('image3.png');
      |}
      """.stripMargin

    val response = CssResponse(cssFile).toResponse(request)
    val result:ParserResult = parser.parse(response)

    result.links.size should equal (3)
    result.links.filter(_.crawlUri.nonEmpty).size should equal (2)
    result.links.filter(_.failMessage.nonEmpty).size should equal (1)

  }

}