package org.ferrit.server.json

import org.ferrit.core.crawler.CrawlConfig

case class Id(id: String)
case class Message(message: String)
case class ErrorMessage(statusCode: Int, message: String)
