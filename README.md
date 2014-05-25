About
-----

Ferrit is an API driven web crawler service written in Scala using [Akka](http://akka.io), [Spray.IO](http://spray.io) and [Cassandra](http://cassandra.apache.org).

It is a personal project and not meant for production use.

If you are learning about web crawlers I think you will find this project interesting.

Other great web crawler projects that have influenced the design include: [Apache Nutch](https://nutch.apache.org), [Scrapy](http://scrapy.org) and [Crawler4J](https://code.google.com/p/crawler4j).


Features
--------

* Designed for [focused web crawling](http://en.wikipedia.org/wiki/Focused_crawler) with separate crawl configurations per website
* REST/JSON API for crawler configuration and starting/stopping jobs
* URI filtering to accept/reject URIs according to rules (using regular expressions)
* URI filter tests can be stored with configurations to guarantee rules work as expected
* Extracts links from HTML and CSS content types (uses [Jsoup](http://jsoup.org))
* Crawl depth is supported
* Supports the [Robots Exclusion Standard](http://www.robotstxt.org) (robots.txt)
* Supports a custom crawl delay per job but always defers to the crawl-delay directive in robots.txt if found
* Requires a [User-agent](http://en.wikipedia.org/wiki/User_agent#Format_for_automated_agents_.28bots.29) property be set to ensure polite crawling
* [URI normalization](http://en.wikipedia.org/wiki/URL_normalization) is applied to links to reduce fetching of duplicate resources
* Supports crawl timeout and max download settings to prevent crawl jobs running on indefinitely
* Fetched content is stored in a database (Cassandra)
* Can run multiple crawl jobs concurrently

Requirements
------------

This project is still at the "builds on my machine" stage (64 bit Windows 8) and needs to be independently tested in a CI environment.

* Java 7/8
* SBT 0.13.2
* Cassandra 2.0.3


Build and Run
-------------

There is no executable Jar with the project. You should checkout the project and build it.
Before starting Ferrit make sure Cassandra is already running.

> On the first occasion you start Cassandra you need to create a new keyspace and tables for Ferrit.
See the [Cassandra Schema File](https://raw.githubusercontent.com/reggoodwin/ferrit/master/src/main/resources/cassandra-schema.sql)
Open the CQL console and paste the contents of the above file into it.

You can build/run Ferrit one of two ways:

(1) Run from within sbt (uses the excellent Spray [sbt-revolver](https://github.com/spray/sbt-revolver) plugin):

    cd <root project directory>
    sbt
    re-start

    // then make API calls to configure crawler
    // run crawls ...
    // when finished stop with:

    re-stop


(2) Assemble and run the executable Jar (uses [sbt-assembly](https://github.com/sbt/sbt-assembly) plugin):

    // Build Ferrit
    // This takes a while, is resource intensive, builds a Jar and places in /bin
    cd <this project directory>
    sbt assembly
  
    // To start Ferrit:
    cd bin
    ferrit

    // To shutdown (in new console window)
    curl -X POST localhost:8080/shutdown

A succesful startup will display:

    Ferrit -                                  _ _
    Ferrit -               ____ __  _ _  _ _ (_| )_
    Ferrit - -------------| __// _)| '_)| '_)| | |_--------------
    Ferrit - -------------| _| \__)|_|--|_|--|_|\__)-------------
    Ferrit - =============|_|====================================
    Ferrit -
    Ferrit - ------------ THE  W E B  C R A W L E R -------------
    Ferrit -
    Cluster - Starting new cluster with contact points [/127.0.0.1]
    ControlConnection - [Control connection] Refreshing node list and token map
    ControlConnection - [Control connection] Refreshing schema
    ControlConnection - [Control connection] Successfully connected to /127.0.0.1
    Session - Adding /127.0.0.1 to list of queried hosts
    Ferrit - Server started on http://localhost:8080


API Documentation
-----------------

> To view API docs, start Ferrit and visit http://localhost:8080 in your browser.

Run your First Crawl Job
------------------------

Here's the 5 minute guide to creating a crawler configuration and starting a crawl job.

(1) First create a new crawler configuration.

This example creates a sample configuration of a crawler that will fetch content from the W3C website. 
Before issuing the POST, insert a user agent string value where you see the "userAgent" property of the JSON sample below. Example:

    Your Name (contact email)

After you POST this configuration, copy the crawlerId property returned in the JSON response.

    curl -XPOST "localhost:8080/crawlers" --header "Content-Type: application/json" -d '{
        "id": "new",
        "crawlerName": "The W3C",
        "seeds": [
            "http://www.w3.org/"
        ],
        "uriFilter": {
            "filterClass": "org.ferrit.core.filter.PriorityRejectUriFilter",
            "rules": [
                "accept: http://www.w3.org/",
                "reject: (?i).*(\\.(jpe?g|png|gif|bmp))$"
            ]
        },
        "tests": [
            "should accept: http://www.w3.org/standards/webdesign/",
            "should reject: http://www.w3.org/2012/10/wplogo_transparent.png"
        ],
        "userAgent": #CHANGE ME#,
        "obeyRobotRules": true,
        "maxDepth": 2,
        "maxFetches": 10,
        "maxQueueSize": 1000,
        "maxRequestFails": 0.2,
        "crawlDelayMillis": 1000,
        "crawlTimeoutMillis": 100000
    }'

(2) Run a new crawl job using this new crawler configuration:

    curl -XPOST "localhost:8080/job_processes" --header "Content-Type: application/json" -d '{
        "id": "#ID-OF-NEW-CRAWLER#"
    }'

The job should start and finish after about a minute because only 10 resources are fetched.
Check the Ferrit console log for progress ...

> IMPORTANT
Web crawlers have a bad reputation on the Internet because they often crawl too aggressively so please use Ferrit politely.
For testing purposes the maximum number of fetches is set to an artificially low 10 pages in the example above.
Please don't increase this, unless coincidentally you really intend to be crawling the W3C website!


What's Missing?
----------------

* Usability! Yes, running a crawler with curl is no fun and error prone. Fortunately there is a user interface project in the works ...
* No [web scraping](http://en.wikipedia.org/wiki/Web_scraper) functionality added (yet), apart from automatic link extraction.
* No content deduplication support
* Redirect responses from servers not (yet) handled properly
* Job clustering not supported
* No backpressure support in the Journal (not really an issue at present because Cassandra writes are so fast)
* JavaScript in HTML pages is not evaluated which means that links in dynamically generated DOM content are not discovered
* Etc ...

Where's the User Interface?
---------------------------

I have a web interface for this service in another Play Framework / AngularJS project.
The code for that other project needs to be cleaned up before publishing.


Known Issues
------------

* For some websites the crawl job will just abort. I have no more information and will need to fix.
* Not all links extracted from pages can be parsed. When unparseable links are found they are logged but not added to the Frontier (aka queue) for fetching.
