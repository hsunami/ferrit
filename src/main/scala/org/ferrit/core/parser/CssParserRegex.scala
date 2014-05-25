package org.ferrit.core.parser

import scala.collection.immutable.Range
import scala.util.matching.Regex.Match
import org.ferrit.core.util.{MediaType, TagUtil, Stopwatch}
import org.ferrit.core.http.Response
import org.ferrit.core.util.TagUtil.{CssTagEquiv, CssUrl, SlashStarComment}
import org.ferrit.core.uri.CrawlUri


/**
 * A CSS link extractor using regular expressions to find links inside 
 * url functions. URLs inside of CSS comments are ignored.
 */
class CssParserRegex extends ContentParser {
  
  override def canParse(response: Response):Boolean = 
    MediaType.is(response, MediaType.Css)
    
  override def parse(response: Response):ParserResult = {

    if (!canParse(response)) throw new ParseException(
      "Cannot parse response"
    )

    val stopwatch = new Stopwatch

    val base = response.request.crawlUri
    val content = response.contentString

    val cssCommentRanges:Seq[Range] = SlashStarComment
        .findAllMatchIn(content)
        .map(m => Range(m.start(1), m.end(1)))
        .toSeq

    def matchWithinComment(start: Int, end: Int) =
      cssCommentRanges.find(r => r.contains(start)) match {
        case Some(range) => range.contains(end)
        case None => false
      }

    var links: List[Link] = List.empty
    
    CssUrl.findAllMatchIn(content)
      .filter(m => !matchWithinComment(m.start(1), m.end(1)))
      .foreach(m => {
          val url = m.group(1)
          val (absUri, failMsg) = try {
            (Some(CrawlUri(base, url)), None)
          } catch {
            case t: Throwable => (None, Some(m.group(0)))
          }
          links = Link(CssTagEquiv, url, "", false, absUri, failMsg) :: links
      })

    DefaultParserResult(links.toSet, false, false, stopwatch.duration)

  }

}