package org.ferrit.core.model

import java.nio.ByteBuffer

case class Document(
  crawlerId: String,
  jobId: String,
  uri: String,
  contentType: String,
  content: Array[Byte]
)