package org.ferrit.core.util

import scala.util.matching.Regex


object TagUtil {
  
  /**
   * Collection of HTML attributes for an HTML link extractor 
   * to search that may contain a URL.
   */
  final val HtmlUriAttributes: Seq[String] = Seq(
    "href",
    "src",
    "cite",
    "data"
  )

  /**
   * Regex to match '@import' url directives, typically within
   * a CSS file or &lt;style&gt; HTML element.
   */
  final val CssImportUrl:Regex = 
    """(?si)@import\s+url\s*\(\s*([\"']?)(.*?)\1\s*\)""".r
  
  final val SlashStarComment:Regex = 
    """(?s)(?i)/\*(.*?)\*/""".r
  
  final val CssUrl:Regex =
    """(?s)(?i)url\s*\(\s*[\"']?(.*?)[\"']?\s*\)""".r

  /**
   * A placeholder synthetic HTML element used in Link for URIs 
   * found in non-HTML documents such as CSS files.
   */
  final val CssTagEquiv:String = "css_url"

}
