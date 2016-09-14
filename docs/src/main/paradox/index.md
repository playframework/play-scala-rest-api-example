# Making a REST API with Play

This is a multi-part guide to walk you through how to make a RESTful API with JSON using [Play 2.5](https://playframework.com).

## Why use Play as a REST API?

Because Play is **built on reactive bedrock**.  Play starts from a reactive core, and builds on reactive principles all the way from the ground.

Play uses a small thread pool, and breaks network packets into a stream of small chunks of data keeps those threads fed with HTTP requests, which means it's fast.  and feeds those through Akka Streams, the Reactive Streams implementation designed by the people who invented Reactive Streams and wrote the Reactive Manifesto.  

In fact, Play is so fast that you have to turn off machines so that the rest of your architecture can keep up.  The Hootsuite team was able to **reduce the number of servers by 80%** by [switching to Play](https://www.lightbend.com/resources/case-studies-and-stories/how-hootsuite-modernized-its-url-shortener).  if you deploy Play with the same infrastructure that you were using for other web frameworks, you are effectively staging a denial of service attack against your own database.

Linkedin uses Play throughout its infrastructure. It wins on all [four quadrants of scalability](http://www.slideshare.net/brikis98/the-play-framework-at-linkedin/128-Outline1_Getting_started_with_Play2) ([video](https://youtu.be/8z3h4Uv9YbE)).  Play's average "request per second" comes in around [tens of k on a basic quad core w/o any intentional tuning](https://twitter.com/kevinbowling1/status/764188720140398592) -- and it only gets better.  

Play provides an easy to use MVC paradigm, including hot-reloading without any JVM bytecode magic or container overhead.  Startup time for a developer on Play was **reduced by roughly 7 times** for [Walmart Canada](https://www.lightbend.com/resources/case-studies-and-stories/walmart-boosts-conversions-by-20-with-lightbend-reactive-platform), and using Play **reduced development times by 2x to 3x**.

Play combines this with a **reactive programming API*** that lets you write async, non-blocking code in a straightforward fashion without worrying about complex and confusing "callback hell."

@@@index

* [Part One](part-1/index.md)
* [Appendix](appendix.md)

@@@
