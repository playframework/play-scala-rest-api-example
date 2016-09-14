# Making a REST API with Play

This is a multi-part guide to walk you through how to make a RESTful API with JSON using [Play 2.5](https://playframework.com).

We’ll demonstrate with a “best practices” REST API that you can clone from [http://github.com/playframework/play-rest-api](http://github.com/playframework/play-rest-api) -- this example is in Scala, but Play also has a Java API which looks and acts just like the Scala API.

Note that there’s more involved in a REST API -- monitoring, representation, and managing access to back end resources -- that we'll cover in subsequent posts.  But first, let's address why Play is so effective as a REST API.

## When to use Play

Play makes a good REST API implementation because Play does the right thing out of the box.  Play makes simple things easy, makes hard things possible, and encourages code that scales because it works in sympathy with the JVM and the underlying hardware. But "safe and does the right thing" is the boring answer.

The fun answer is that Play is **fast**.

In fact, Play is so fast that you have to turn off machines so that the rest of your architecture can keep up.  The Hootsuite team was able to **reduce the number of servers by 80%** by [switching to Play](https://www.lightbend.com/resources/case-studies-and-stories/how-hootsuite-modernized-its-url-shortener).  if you deploy Play with the same infrastructure that you were using for other web frameworks, you are effectively staging a denial of service attack against your own database.

Play is fast because Play is **built on reactive bedrock**.  Play starts from a reactive core, and builds on reactive principles all the way from the ground.  Play uses a small thread pool, and breaks network packets into a stream of small chunks of data keeps those threads fed with HTTP requests, which means it's fast.  and feeds those through Akka Streams, the Reactive Streams implementation designed by the people who invented [Reactive Streams](http://www.reactive-streams.org/) and wrote the [Reactive Manifesto](http://www.reactivemanifesto.org/).

Linkedin uses Play throughout its infrastructure. It wins on all [four quadrants of scalability](http://www.slideshare.net/brikis98/the-play-framework-at-linkedin/128-Outline1_Getting_started_with_Play2) ([video](https://youtu.be/8z3h4Uv9YbE)).  Play's average "request per second" comes in around [tens of k on a basic quad core w/o any intentional tuning](https://twitter.com/kevinbowling1/status/764188720140398592) -- and it only gets better.

Play provides an easy to use MVC paradigm, including hot-reloading without any JVM bytecode magic or container overhead.  Startup time for a developer on Play was **reduced by roughly 7 times** for [Walmart Canada](https://www.lightbend.com/resources/case-studies-and-stories/walmart-boosts-conversions-by-20-with-lightbend-reactive-platform), and using Play **reduced development times by 2x to 3x**.

Play combines this with a **reactive programming API** that lets you write async, non-blocking code in a straightforward fashion without worrying about complex and confusing "callback hell."  In either Java or Scala, Play works on the same principle: leverage the asynchronous computation API that the language provides to you.  In Play, you work with `java.util.concurrent.CompletionStage` or `scala.concurrent.Future` API directly, and Play passes that asynchronous computation back through the framework.

Finally, Play is modular and extensible.  Play works with multiple runtime and compile time dependency injection frameworks like [Guice](https://www.playframework.com/documentation/2.5.x/ScalaDependencyInjection), [Macwire](https://di-in-scala.github.io/), [Dagger](https://github.com/esfand-r/play-java-dagger-dependency-injection#master), and leverages DI principles to integrate authentication and authorization frameworks built on top of Play.

## Running, Using and Load Testing

For instructions on running and using the project, please see the [appendix](appendix.md).

Interested in load testing this project?  It comes with an integrated [Gatling](http://gatling.io/) load test.  Again, instructions are in the [appendix](appendix.md).

## Things Not Covered By This Guide

One thing to note here is that although this guide covers how to make a REST API in Play, it only covers Play itself and deploying Play on a single server.  It does not cover larger scale concerns about microservices such as ensuring resiliency, persistence, distributing work over multiple machines, or monitoring.

For full scale microservices, you want [Lagom](http://www.lagomframework.com/), which is an "industrialized" Play -- a microservices framework set up with built in persistence and service APIs set up to ensure that the service always stays up and responsive even in the face of chaos monkeys and network partitions.

With that caveat, let's start working with Play!

@@@index

* [Basics](part-1/index.md)
* [Appendix](appendix.md)

@@@

## Community

To learn more about Play, check out the [Play tutorials](https://playframework.com/documentation/2.5.x/Tutorials) and see more examples and blog posts about Play, including streaming [Server Side Events](https://github.com/playframework/play-streaming-scala) and first class [WebSocket support](https://github.com/playframework/play-websocket-scala).

To get more involved and if you have questions, join the [mailing list](https://groups.google.com/forum/#!forum/play-framework) at  and follow [PlayFramework on Twitter](https://twitter.com/playframework).
