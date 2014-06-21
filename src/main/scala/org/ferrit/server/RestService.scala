package org.ferrit.server

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import play.api.libs.json.Json
import spray.can.Http
import spray.can.server.Stats
import scala.concurrent.duration._
import spray.http.ContentTypes
import spray.http.MediaTypes.`text/html`
import spray.http.StatusCodes
import spray.httpx.unmarshalling._
import spray.httpx.marshalling._
import spray.httpx.PlayJsonSupport._
import spray.http.MediaTypes._
import spray.routing.{ExceptionHandler, HttpService}
import spray.util.LoggingContext
import spray.util._ // to resolve "actorSystem"
import reflect.ClassTag // workaround, see below
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.ferrit.core.crawler.{CrawlerManager, CrawlConfig, CrawlLog, CrawlerException}
import org.ferrit.core.crawler.{CrawlConfigValidator, CrawlConfigValidatorResults, CrawlConfigValidatorResult}
import org.ferrit.core.crawler.CrawlerManager.{StartJob, JobStartFailed}
import org.ferrit.core.crawler.CrawlerManager.{StopJob, StopAllJobs, StopAccepted}
import org.ferrit.core.crawler.CrawlerManager.{JobsQuery, JobsInfo}
import org.ferrit.core.model.{Crawler, CrawlJob}
import org.ferrit.core.json.PlayJsonImplicits._
import org.ferrit.core.uri.CrawlUri
import org.ferrit.dao.{DAOFactory, CrawlJobDAO, CrawlerDAO, DocumentMetaDataDAO, FetchLogEntryDAO, Journal}
import org.ferrit.server.json.{Id, Message, ErrorMessage}
import org.ferrit.server.json.PlayJsonImplicits._

/**
 * Exposes crawler functionality as a REST API service.
 */
class RestService(

  mainActor: ActorRef, 
  daoFactory: DAOFactory, 
  crawlerManager: ActorRef

  ) extends Actor with HttpService {

  implicit def executionContext = actorRefFactory.dispatcher
  def actorRefFactory = context
  def receive = runRoute(routes)
  val askTimeout = new Timeout(3.seconds)

  val jobDao: CrawlJobDAO = daoFactory.crawlJobDao
  val crawlerDao: CrawlerDAO = daoFactory.crawlerDao
  val fleDao: FetchLogEntryDAO = daoFactory.fetchLogEntryDao
  val docMetaDao: DocumentMetaDataDAO = daoFactory.documentMetaDataDao

  val webDirectory = "web"

  val routes = {
    pathSingleSlash {
      getFromResource(s"$webDirectory/index.html")
    } ~
    pathPrefix("css") {
      getFromResourceDirectory(s"$webDirectory/css")  
    } ~
    pathPrefix("js") {
      getFromResourceDirectory(s"$webDirectory/js")
    } ~
    path("crawlers" / Segment / "jobs" / Segment / "fetches") { (crawlerId, jobId) =>
      get {
        rejectEmptyResponse {
          complete {
            val fetches = fleDao.find(jobId)
            val opt = if (fetches.isEmpty) None else Some(fetches)
            opt
          }
        }  
      }
    } ~
    path("crawlers" / Segment / "jobs" / Segment / "assets") { (crawlerId, jobId) =>
      get {
        rejectEmptyResponse {
          complete {
            val docs = docMetaDao.find(jobId)
            val opt = if (docs.isEmpty) None else Some(docs)
            opt
          }
        }  
      }
    } ~
    path("crawlers" / Segment / "jobs") { (crawlerId) =>
      get {
        rejectEmptyResponse {
          complete {
            val opt = jobDao.find(crawlerId)
            opt
          }
        }  
      }
    } ~
    path("crawlers" / Segment / "jobs" / Segment) { (crawlerId, jobId) =>
      get {
        rejectEmptyResponse {
          complete {
            val opt = jobDao.find(crawlerId, jobId)
            opt
          }
        }  
      }
    } ~
    path("crawlers" / Segment) { crawlerId =>
      post {
        entity(as[CrawlConfig]) { config =>
          complete {
            if ("config_test" == crawlerId) {
              val results: CrawlConfigValidatorResults = CrawlConfigValidator.testConfig(config)
              results.allPassed match {
                case true => StatusCodes.OK -> results
                case false => StatusCodes.BadRequest -> results
              }
            } else {
              StatusCodes.BadRequest -> Message(s"Cannot post to named crawler resource")
            }
          }
        }  
      } ~
      get {
        rejectEmptyResponse {
          complete {
            val opt = crawlerDao.find(crawlerId) match {
              case Some(crawler) => Some(crawler.config)
              case None => None
            }
            opt
          }
        }  
      } ~
      put {
        entity(as[CrawlConfig]) { config =>
          complete {
            val config2 = config.copy(id = crawlerId)
            val crawler = Crawler(crawlerId, config2)
            crawlerDao.insert(crawler)
            StatusCodes.Created -> config2
          }  
        }
      } ~
      delete {
        rejectEmptyResponse {
          complete {
            val opt = crawlerDao.find(crawlerId) match {
              case Some(crawler) =>
                crawlerDao.delete(crawlerId)
                Some(StatusCodes.NoContent -> "")
              case None => None
            }
            opt
          }
        }
      }
    } ~
    path("crawlers") {
      get {
        complete {
          val crawlers:Seq[Crawler] = crawlerDao.findAll()
          crawlers.map(c => c.config)
        }
      } ~
      post {
        entity(as[CrawlConfig]) { config: CrawlConfig =>
          complete {
            val id = java.util.UUID.randomUUID().toString()
            val newConfig = config.copy(id=id)
            val crawler = Crawler(id, newConfig)
            crawlerDao.insert(crawler)
            StatusCodes.Created -> newConfig
          }
        }  
      }
    } ~
    path("jobs") {
      get {
        parameter("date" ? "default") { dateKey =>
          complete {
            val partitionKey = dateKey match {
              case "default" => new DateTime
              case dk => DateTimeFormat.forPattern("YYYY-MM-dd").parseDateTime(dateKey)
            }
            jobDao.find(partitionKey.withTimeAtStartOfDay)
          }
        }
      }
    } ~
    path("job_processes") {
      post {

        // Provide generous Timeout when starting job. Seeds need enqueing
        // which in turn requires fetching robots.txt to be sure they are valid.
        
        val startJobTimeout = new Timeout(30.seconds)

        entity(as[Id]) { id =>
          complete {
            crawlerDao.find(id.id) match {
              case Some(Crawler(crawlerId, config)) =>
                val logger = context.actorOf(Props[CrawlLog])
                val fetchLogger = context.actorOf(Props(classOf[Journal], daoFactory))    
                crawlerManager
                  .ask(StartJob(config, Seq(logger, fetchLogger)))(startJobTimeout)
                  .mapTo[CrawlJob]
                  .map({j => j })
              case _ => StatusCodes.BadRequest -> Message(s"No crawler found with identifier [${id.id}]")
            }
          }
        }
      } ~
      get {
        complete {
          crawlerManager
            .ask(JobsQuery())(askTimeout)
            .mapTo[JobsInfo]
            .map({jobsInfo => jobsInfo.jobs })
        }
      } ~
      delete {
        complete {
          crawlerManager
            .ask(StopAllJobs())(askTimeout)
            .mapTo[StopAccepted]
            .map({sa => Message(s"Stop request accepted for ${sa.ids.size} jobs") })
        }
      }
    
    } ~
    path("job_processes" / Segment) { jobId =>
      delete {
        complete {
          crawlerManager
            .ask(StopJob(jobId))(askTimeout)
            .mapTo[StopAccepted]
            .map({jobId => Message(s"Stop request accepted for job [$jobId]") })
        }
      }
    
    } ~
    path("shutdown") {
      post {
        complete {
          val delay = 1.second
          actorSystem.scheduler.scheduleOnce(delay) {
            mainActor ! Ferrit.Shutdown
          }
          Message("Shutdown hook received")
        }  
      }
    }
  }

  implicit def myExceptionHandler(implicit log: LoggingContext) = 
    ExceptionHandler {
      case t: CrawlerException =>
        requestUri { uri =>
          val error = ErrorMessage(500, "Apologies, an internal error occurred")
          complete(StatusCodes.InternalServerError, error)
        }
    }

}