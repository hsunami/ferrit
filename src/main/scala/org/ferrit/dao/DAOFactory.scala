package org.ferrit.dao

import com.datastax.driver.core.Session
import org.ferrit.dao.cassandra.CassandraColumnTTL
import org.ferrit.dao.cassandra._


class DAOFactory(ttl: CassandraColumnTTL, session: Session) {
  
  private [dao] implicit val _session = session

  import org.ferrit.core.model._

  // Cassandra prepared statements should only be created once.
  // The driver complains if they are created multiple times
  // which happens if there are multiple DAOs created, yet a Session
  // is required to create prepared statements which first requires
  // a DAO instance.

  lazy val crawlerDao = new CassandraCrawlerDAO(ttl)
  lazy val crawlJobDao = new CassandraCrawlJobDAO(ttl)
  lazy val fetchLogEntryDao = new CassandraFetchLogEntryDAO(ttl)
  lazy val documentMetaDataDao = new CassandraDocumentMetaDataDAO(ttl)
  lazy val documentDao = new CassandraDocumentDAO(ttl)

}

