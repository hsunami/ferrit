About
-----

Hi and welcome to the Ferrit project, an API driven web crawler service written in Scala using [Akka](http://akka.io), [Spray.IO](http://spray.io) and [Cassandra](http://cassandra.apache.org).

I wrote Ferrit to help me learn about small service design using Akka and to help up my game with the *Functional Reactive* programming style. If you are also learning about these things then I hope you will find a few things of interest to you.


Features
--------

Ferrit is a [focused web crawler](http://en.wikipedia.org/wiki/Focused_crawler) with separate crawl configurations and jobs per website. For a whole-internet style of crawler there is Nutch.

* There is a REST/JSON API for crawler configuration and starting/stopping jobs
* All data is stored in Cassandra.
* A novel feature: configuring URI filters can be tricky. Ferrit lets you verify your filter rules ahead of time with tests to ensure that resources will either be fetched or not fetched as expected.


What else is there?

* *Politeness* is pretty important. Ferrit supports the [Robots Exclusion Standard](http://www.robotstxt.org) (robots.txt) and although a custom fetch delay can be set per job the final decision should be with the webmaster as to how fast you are allowed to crawl their site (the crawl-delay directive in robots.txt). Crawlers require a [User-agent](http://en.wikipedia.org/wiki/User_agent#Format_for_automated_agents_.28bots.29) property be set so that webmasters can contact the owner if necessary.
* *Crawl depth* is supported as a way to restrict fetches to the top N pages of a given website
* *URI filtering* is handy for deciding which resources should be fetched, e.g. fetch only HTML pages (uses accept/reject rules with regular expressions).
* *URI normalization* - this is tricky to get right and I'm not entirely there yet. URI normalization is required to help reduce fetching of duplicate resources.
* *Spider trap* prevention: for various reasons crawl jobs can run on indefinitely. Crawlers can be configured with basic safeguards such as a *crawl timeout* and *max fetches*.
* *Concurrent job* support - this is pretty basic so far, needs more exploration.


Requirements
------------

This project is still at the [builds on my machine](http://www.buildsonmymachine.com/) stage (64 bit Windows 8) and needs to be independently tested in a Linux environment at Amazon EC2.

* Java 7/8
* Scala 2.10
* SBT 0.13.2
* Cassandra 2.0.3


Build and Run
-------------

There is no binary file with this project so just checkout and build with sbt. It should only take a few minutes.

> B.t.w. before starting Ferrit do make sure Cassandra is already running. The first time you run the project you'll need to create a new keyspace and tables in Cassandra. Just open this [Cassandra schema file](https://raw.githubusercontent.com/reggoodwin/ferrit/master/src/main/resources/cassandra-schema.sql), open the CQL console and paste the contents of the file into it.

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
    curl -X POST localhost:6464/shutdown

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
    Ferrit - Server started on http://localhost:6464



The host and port settings can be changed in: src/main/resources/application.conf.

    app {
      server {
      host = "127.0.0.1"
      port = 6464
    }


API Documentation
-----------------

> To view API documentation, start Ferrit and visit http://localhost:6464 in your browser. I will likely move them over to this page in a future update.


How to Run a Crawl Job
------------------------

Here's the 5 minute guide to using the API to create a crawler configuration and start a crawl job.
(There is also a user interface project on the way which is preferable to operating with curl).

(1) First create a new crawler configuration.

This example creates a sample configuration of a crawler that will fetch content from the W3C website. 
Before issuing the POST, insert a user agent string value where you see the "userAgent" property of the JSON sample below. Example:

    Your Name (contact email)

After you POST this configuration, copy the crawlerId property returned in the JSON response.

    curl -XPOST "localhost:6464/crawlers" --header "Content-Type: application/json" -d '{
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

    curl -XPOST "localhost:6464/job_processes" --header "Content-Type: application/json" -d '{
        "id": "#ID-OF-NEW-CRAWLER#"
    }'


The job should start and finish after about a minute because only 10 resources are fetched.
Check the Ferrit console log for progress ...

Good luck!

> IMPORTANT -
web crawlers get a bad reputation because they often crawl too aggressively so please use Ferrit politely. For the example above the maximum number of fetches is set to an artificially low 10 pages. Please don't increase this unless coincidentally you really intend to be crawling the W3C website! I test crawler functionality against a small website running on Apache locally before scaling up to real websites.


More About Persistence
----------------------

Each crawl job generates a completely separate document set. That means URI uniqueness is scoped at the job level. This design choice means that writes can be super fast because there is no need for a *read-before-write* to see if a resource should be updated rather than inserted (read-before-write is an anti-pattern in Cassandra anyway). Fetched documents have a time-to-live setting so that old content is removed automatically over time without needing a manual clean up process.


What's Missing?
---------------

* Usability! Yes, running a crawler with curl is no fun and rather error prone. Fortunately there is a user interface project in the works that will be sure to work wonders ...
* No [web scraping](http://en.wikipedia.org/wiki/Web_scraper) functionality added (yet), apart from automatic link extraction. So in a nutshell you can't actually do anything with this crawler except crawl.
* No content deduplication support
* Redirect responses from servers are not (yet) handled properly
* Job clustering is not supported
* No backpressure support in the Journal (not an issue at present because Cassandra writes are so fast)
* JavaScript is not evaluated so links in dynamically generated DOM content are not discovered.


Where's the User Interface?
---------------------------

I have a web interface for this service in another Play Framework / AngularJS project.
The code for that other project needs to be cleaned up before publishing.


Influences
----------

Guess you already know about those other great open source web crawler projects out there which have influenced this design. First there is the granddaddy of the Java pack [Apache Nutch](https://nutch.apache.org). Then also there is the mature [Scrapy](http://scrapy.org) project which makes me yearn to be able to program Python! I also found that [Crawler4J](https://code.google.com/p/crawler4j) is good to study when first starting out because there is not too much code to review.


Known Issues
------------

Tip of the iceberg:

* Most of the websites I have crawled are fine except for a few where the crawl job just aborts. I need to to fix that.
* Not all links extracted from pages can be parsed. When unparseable links are found they are logged but not added to the Frontier (aka queue) for fetching.
