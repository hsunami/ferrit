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

Missing Features
----------------

* No [web scraping](http://en.wikipedia.org/wiki/Web_scraper) functionality added (yet), apart from automatic link extraction
* No content deduplication support
* Redirect responses from servers not (yet) handled properly
* Job clustering not supported
* No backpressure support in the Journal (not really an issue at present because Cassandra writes are so fast)
* JavaScript in HTML pages is not evaluated which means that links in dynamically generated DOM content are not discovered


Where's the User Interface?
---------------------------

I have a web interface for this service in another Play Framework project using AngularJS.
The code for that other project needs to be cleaned up before publishing.


Installation
------------

To do ...

How To Use
----------

To do ...


Known Issues
------------

* Not all links extracted from pages can be parsed. When unparseable links are found they are logged but not added to the Frontier (aka queue) for fetching.
