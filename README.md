# REST API

Imagine a REST API for displaying blog posts, containing a `Post` resource.  This resource can be manipulated with HTTP verbs as is normal for a REST API.
  
* `GET /posts` shows a list of blog posts.  
* `GET /post/1` shows a single blog post.  
* `POST /posts` creates a new blog post.

[Play Framework](https://www.playframework.com/documentation/2.5.x) does this out of the box -- in fact, Play leverages REST principles from the ground up to work directly against HTTP, without any additional layers or specialized annotations, so working with REST is fun.

However, a REST API is more than just fun.  When people ask for a REST API, they're often asking for multiple features which some frameworks should be able to provide, but not all do.  A good REST API frameworks means the ability to do a number of things:

* It means being able to run securely over HTTPS out of the box.
* It means being able to use a lightweight "route to function" style popularized by Sinatra and Spray.
* It means being able to seamless wrap execution logic with logging and metrics.
* It means being able to use non-blocking code even when using blocking JDBC databases.
* It means being able to correctly and concisely compose [RESTful principles](http://www.vinaysahni.com/best-practices-for-a-pragmatic-restful-api).
* It means being able to provide multiple representations of a resource depending on the content type.
* It means being able to start simply and cleanly and never having to rewrite for scalability.

Play does all of this, and is used as the standard REST API for services infrastructure at companies like [LinkedIn](https://www.lightbend.com/resources/case-studies-and-stories/the-play-framework-at-linkedin) and [Hootsuite](https://www.lightbend.com/resources/case-studies-and-stories/how-hootsuite-modernized-its-url-shortener). 

We'll demonstrate with a "best practices" REST API that you can clone from [http://github.com/playframework/play-rest-api](http://github.com/playframework/play-rest-api).  

Note that this is an example rather than a framework and so it won't cover everything.  There are other examples we'll point to at the end of this document that cover pagination, sorting, searching and authentication.
 
## Running

 You need to [download and install](http://www.scala-sbt.org/0.13/docs/Setup.html) `sbt` for this application to run.

Once you have sbt installed, use the following command:

```
sbt run
```

Play will start up on two ports: the HTTP port at [http://localhost:9000/](http://localhost:9000/) and the HTTPS port at [https://localhost:9443/](https://localhost:9443/).  Going to the HTTP port will immediately redirect you to the HTTPS port, which has a self-signed certificate. 

## Usage

There are two ways to navigate the REST API.  If you go to [https://localhost:9443/posts](https://localhost:9443/posts), then you'll see a list of posts rendered in HTML, using Bootstrap CSS.

However, if you call the same URL from the command line, you'll see JSON.  Using [httpie](https://github.com/jkbrzt/httpie), we can execute the command:


```
http --verify=no  --verbose https://localhost:9443/posts
```

and get back:

```
GET /posts HTTP/1.1
Accept: */*
Accept-Encoding: gzip, deflate
Connection: keep-alive
Host: localhost:9443
User-Agent: HTTPie/0.9.2



HTTP/1.1 200 OK
Cache-Control: max-age: 300
Content-Length: 513
Content-Security-Policy: default-src 'self'
Content-Type: application/json
Date: Tue, 02 Aug 2016 20:13:34 GMT
Strict-Transport-Security: max-age=300
Vary: Accept
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-Permitted-Cross-Domain-Policies: master-only
X-XSS-Protection: 1; mode=block

[
    {
        "body": "blog post 1", 
        "comments": [
            {
                "body": "comment 1"
            }, 
            {
                "body": "comment 2"
            }
        ], 
        "id": "1", 
        "link": "/posts/1", 
        "title": "title 1"
    }, 
    {
        "body": "blog post 2", 
        "comments": [], 
        "id": "2", 
        "link": "/posts/2", 
        "title": "title 2"
    }, 
    {
        "body": "blog post 3", 
        "comments": [
            {
                "body": "comment 3"
            }, 
            {
                "body": "comment 4"
            }
        ], 
        "id": "3", 
        "link": "/posts/3", 
        "title": "title 3"
    }, 
    {
        "body": "blog post 4", 
        "comments": [], 
        "id": "4", 
        "link": "/posts/4", 
        "title": "title 4"
    }, 
    {
        "body": "blog post 5", 
        "comments": [
            {
                "body": "comment 5"
            }
        ], 
        "id": "5", 
        "link": "/posts/5", 
        "title": "title 5"
    }
]
```

Likewise, you can create a post through HTML using the standard [Play Form API](https://www.playframework.com/documentation/2.5.x/ScalaForms), but you can also send a POST directly as JSON:

```
http --verify=no  --verbose POST https://localhost:9443/posts title="hello" body="world"
```

and get:

```
POST /posts HTTP/1.1
Accept: application/json
Accept-Encoding: gzip, deflate
Connection: keep-alive
Content-Length: 35
Content-Type: application/json
Host: localhost:9443
User-Agent: HTTPie/0.9.2

{
    "body": "world", 
    "title": "hello"
}

HTTP/1.1 201 Created
Content-Length: 12
Content-Security-Policy: default-src 'self'
Content-Type: application/json
Date: Tue, 02 Aug 2016 20:22:01 GMT
Strict-Transport-Security: max-age=300
Vary: Accept
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-Permitted-Cross-Domain-Policies: master-only
X-XSS-Protection: 1; mode=block

{
    "id": "999"
}

```

## General Architecture
 
If you look at the application, you'll see that there's a file `app/post/Post.scala`.  There are only two classes in there, `Post` and `Comment` -- these are the DTOs that contain the information that should be displayed to the user.  


``` scala
case class Post(id: String, link: String, title: String, body: String, comments: Seq[Comment])
```

``` scala
case class Comment(body: String)
```

So where does this data in these DTOs come from?

On the backend, there are two repositories, `PostRepository` and `CommentRepository`.  `PostRepository` contains `PostData`, but does not know anything about comments.  Likewise, `CommentRepository` knows about `CommentData`, but only contains the ID of the Post.  For the purposes of this project, we'll pretend that both `PostRepository` and `CommentRepository` are backed by SQL databases, connecting through a JDBC connection pool.  Because JDBC is a blocking model, we define custom execution contexts which are sized to the JDBC connection pools, and we'll make all the methods expose `Future` so that the result is non-blocking.

The `PostRouter` handles all the incoming HTTP requests, looks at the desired representation (JSON or HTML), and then renders a `Post` by querying both the `PostRepository` and the `CommentRepository` and composing the `PostData` and `CommentData` into a `Post` containing the comments, all within a `Future`.  Under the hood, the `PostAction` handles the unglamorous work of providing the underlying machinery -- logging, metrics, and cache control directives -- that are vital to managing a REST API effectively.


## Post and Comments Repositories

The post and comment repositories are defined as non-blocking interfaces on top of a blocking implementation.  Blocking means that a thread is waiting for another thread to release a lock, and cannot do anything.  A blocking API looks like this:

```
trait BlockingRepository {
  def callDatabaseAndWaitForData(): MyData
}
```

In this example, calling `callDatabaseAndWaitForData` means that nothing happens until the database gets back to you.  The way to make something non-blocking is to wrap it in a [Future](http://docs.scala-lang.org/overviews/core/futures.html), which says that the result might come in later, meaning that the thread is freed up to do other work.  A non-blocking API looks like this:

```
trait NonBlockingRepository {
  def callDatabaseAndImmediatelyReturn: Future[MyData]
}
```

When `callDatabaseAndImmediatelyReturn` is called, then a Future is created, and the work of getting MyData back will be assigned to another thread.  The calling thread doesn't have to worry about it, and can go do other stuff.

With that in mind, this is what the repositories look like:

```
final case class PostData(id: PostId, title: String, body: String)

trait PostRepository {

  def list(): Future[Iterable[PostData]]

  def get(id: PostId): Future[Option[PostData]]
}
```

```
final case class CommentData(id: CommentId, postId: String, body: String)

trait CommentRepository {

  def findByPost(postId: String): Future[Iterable[CommentData]]

  def list(): Future[Iterable[CommentData]]

  def get(id: CommentId): Future[Option[CommentData]]

}
```

## Using Custom ExecutionContexts for Blocking I/O

It's important to note that a Future by itself doesn't do any work.  Instead, a Future needs access to an underlying engine which will do the work of executing that Future -- technically an [Executor](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Executor.html), but generically a "thread pool".  When a Future is ready to be executed, then the Executor will run the Future on one of the available threads in the pool.  If there are more Futures than there are available threads, then work will pile up in the queue until the Executor can deal with it.  The way that a Future gets access to an Executor is through an [ExecutionContext](http://www.scala-lang.org/api/2.11.8/#scala.concurrent.ExecutionContext).


``` scala
val runMeInSomeLargeThreadPool: ExecutionContext = ...

val futureResult: Future[Result] = Future {
  // some blocking code
}(runMeInSomeLargeThreadPool)
```

It is common for ExecutionContext to be provided implicitly:


``` scala
implicit val ec: ExecutionContext = ...

val futureResult: Future[Result] = action.async { implicit request =>
  Future {
    // some blocking code
  }(ec)
}
```

However, there can be a problem when an implicit ExecutionContext has a broader lexical scope than intended.  If you have a custom execution context, it's much better to ensure that it's strongly typed, so that an inappropriate ExecutionContext can't be used by accident.  We can do this in Scala by using a value type:

```
class PostExecutionContext(val underlying: ExecutionContext) extends AnyVal
```

And then the PostExecutionContext is injected so that the implicit ExecutionContext is only available to the class:

```
@Singleton
class PostRepositoryImpl @Inject()(pec: PostExecutionContext) extends PostRepository {
  private implicit val ec: ExecutionContext = pec.underlying

}
```

The configuration of the thread pool backing the repository execution context is done through an [Akka Dispatcher](http://doc.akka.io/docs/akka/current/scala/dispatchers.html) in configuration -- we'll get to that in a few sections.  For now, the important thing  is that the repositories expose a `Future` based interface to best leverage Play's architecture, and have a custom ExecutionContext powering those Futures.

## Routing With SIRD

The `PostRouter` is a specialized router that handles the HTTP requests under the `/posts` prefix. Play's [routing DSL](https://www.playframework.com/documentation/2.5.x/ScalaSirdRouter) (aka SIRD) shows how data can be extracted from the URL concisely and cleanly:

``` scala
import javax.inject.Inject

import play.api.mvc._
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._

class PostRouter @Inject()(action: PostAction /* ... */)(implicit ec: ExecutionContext)
  extends SimpleRouter
{
  override def routes: Routes = {

    case GET(p"/") =>
      action.async { implicit request =>
        renderPosts()
      }

    case HEAD(p"/") =>
      action.async { implicit request =>
        renderPosts()
      }

    case POST(p"/") =>
      action.async { implicit request =>
        processPost()
      }

    case GET(p"/$id") =>
      action.async { implicit request =>
        renderPost(PostId(id))
      }

    case HEAD(p"/$id") =>
      action.async { implicit request =>
        renderPost(PostId(id))
      }
  }
}
```

SIRD is based around HTTP verbs and a string interpolated extractor object -- this means that when we type the string "/$id" and prefix it with "p", then the path parameter `id` can be extracted and used in the block.  Naturally, there are also operators to extract queries, regular expressions, and even add custom extractors.  Cake Solutions covers SIRD in depth in a [fantastic blog post](http://www.cakesolutions.net/teamblogs/all-you-need-to-know-about-plays-routing-dsl). 

## Providing a non-blocking context with action.async

Once the parameter has been extracted, the `PostAction` is executed with the `async` method and a block of code which has a `PostRequest` passed in as `implicit` -- this means that any in-scope method that takes an implicit request as a parameter will use this request automatically.  Play is non-blocking all the way through internally, but if you work with blocking code, `async` provides a non-blocking context that returns a `Future[Result]`, so this tells Play we may be passing along a `Future` that started from somewhere else that might be using a blocking model.


``` scala
action.async { implicit request =>
   renderPosts()
}
```

You can read more about this in the [handling asynchronous results](https://www.playframework.com/documentation/2.5.x/ScalaAsync) section.

Play has its own rendering execution context, which is set up with a small number of threads mapped to the number of CPUs.  This is great for handling internal work, and can be injected by declaring an implicit `ExecutionContext` as one of the class parameters. 

``` scala
class PostRouter @Inject()(...)(implicit ec: ExecutionContext)
```

This execution context provides the underlying thread pool for the methods working with futures in `PostRouter` itself.

## Negotiating Content 

Play knows whether to return HTML or JSON because it keeps track of an HTTP header called `Accept`, and provides a [Content Negotiation](https://www.playframework.com/documentation/2.5.x/ScalaContentNegotiation) API that can be used to render different content based on the content type:

``` scala
  private def renderPosts[A]()(implicit request: PostRequest[A]): Future[Result] = {
    render.async {

      case Accepts.Json() & Accepts.Html() if request.method == "HEAD" =>
        // HEAD has no body, so just say hi
        Future.successful(Results.Ok)

      case Accepts.Json() =>
        // Query the repository for available posts
        postRepository.list().flatMap { postDataList =>
          findPosts(postDataList).map { posts =>
            val json = Json.toJson(posts)
            Results.Ok(json)
          }
        }

      case Accepts.Html() =>
        // Query the repository for available posts
        postRepository.list().flatMap { postDataList =>
          findPosts(postDataList).map { posts =>
            Results.Ok(views.html.posts.index(posts, form))
          }
        }

    }
  }
```

Here, we can see that for both code paths, we call postRepository.list() to return a `Future[Iterable[PostData]`.  Calling `flatMap` on that `Future` provides us an `Iterable[PostData]` that we can work with, and from there we can work down to the point where we have `posts`, which is a list of `Post`.  For JSON requests, we call `Json.toJson` to produce the response body, but for HTML we render the output from an [HTML template](https://www.playframework.com/documentation/2.5.x/ScalaTemplates).
  
There's a lot going on here, so let's break it down piece by piece.

## Composing Futures

First, let's talk about `map` and `flatMap`.

One of Play's most powerful features is its ability to [leverage threads](https://www.lightbend.com/blog/why-is-play-framework-so-fast) so that when a CPU core has available cycles, the application can keep handing it work to do -- this is because Play is built on top of Netty, which uses [non-blocking IO](https://en.wikipedia.org/wiki/Non-blocking_I/O_(Java)) (NIO) and the [Reactor pattern](https://en.wikipedia.org/wiki/Reactor_pattern) under the hood.  This is in contrast to the Java Servlet model used in JEE applications that use the "thread per request" blocking model, which hold onto the CPU core for the full duration of the request while data trickles in from the network.  

As discussed earlier in the repositories section, Play packages work into blocks that can be executed when a core is available, and makes those blocks available through a construct called a `Future`.  Because not all the data is available at once, Play makes it easy to chain or "compose" futures together so that the output from one `Future` can be the input to another `Future`, returning another, larger `Future` which be a different type.  This is done using the `map` method, so 

``` scala
val futureFoo: Future[Foo] = Future.successful(foo)
val futureBar: Future[Bar] = future.map { fooResult: Foo =>
  new Bar(foo)
}
```

There are cases where a block of code may return a `Future[Bar]` instead of a `Bar`, meaning that you'd have the following:


``` scala
val futureFoo: Future[Foo] = Future.successful(foo)
val futureFutureBar: Future[Future[Bar]] = future.map { fooResult: Foo =>
  Future { new Bar(foo) }
}
```

This is a bit awkward, but there is a solution.  Because the Future of a Future is a Future, `Future[Future[Bar]]` is essentially the same as `Future[Bar]`. To flatten the Future, we can use the `flatMap` method:

``` scala
val futureFoo: Future[Foo] = Future.successful(foo)
val futureBar: Future[Bar] = future.flatMap { fooResult: Foo =>
  Future { new Bar(foo) }
}
```

In general, the only composition you need to do is to avoid blocking is to use `map` or `flatMap`, and be able to return a `Future[T]`, where T is the type of the value you want. 

So here we've got a classic case of carrying futures around.  We've got two repositories that return `PostData`, and `CommentData`, but they exist wrapped in their own `Future`.  We start by calling `flatMap` on the `Future[PostData]`, and passing it as `postDataList` to a block:

``` scala
postRepository.list().flatMap { postDataList =>
  findPosts(postDataList).map { posts =>
    val json = Json.toJson(posts)
    Results.Ok(json)
  }
}
```

Then, we can go into the `findPosts` method:


``` scala
  private def findPosts(postDataList: Iterable[PostData]): Future[Iterable[Post]] = {
    // Get an Iterable[Future[Post]] containing comments
    val listOfFutures = postDataList.map { p =>
      findComments(p.id).map { comments =>
        Post(p, comments)
      }
    }

    // Flip it into a single Future[Iterable[Post]]
    Future.sequence(listOfFutures)
  }

  private def findComments(postId: PostId): Future[Seq[Comment]] = {
    // Find all the comments for this post
    commentRepository.findByPost(postId.toString).map { comments =>
      comments.map(c => Comment(c.body)).toSeq
    }
  }
```

Here, for each post, we call out to the comment repository for the attached comments, and get a list of futures back.  Then, we flip the result with `Future.sequence`, which turns a list of Future into a single Future that contains a list.  



## Rendering Content as JSON

Play handles the work of converting a `Post` DTO through [Play JSON](https://www.playframework.com/documentation/2.5.x/ScalaJson).  Play JSON provides a DSL that looks up the conversion from the singleton object `Post`:

```
object Post {
  implicit val implicitWrites = new Writes[Post] {
    def writes(post: Post): JsValue = {
      Json.obj(
        "id" -> post.id,
        "link" -> post.link,
        "title" -> post.title,
        "body" -> post.body,
        "comments" -> Json.toJson(post.comments)
      )
    }
  }
}
```

This means that typing `val json: JsValue = Json.toJson(post:Post)` will just work, without any additional imports or setup.  

## Rendering Content as HTML

Play's [HTML Templates](https://www.playframework.com/documentation/2.5.x/ScalaTemplates), also known as Twirl, work very much like JSP pages.  When Play sees a file "app/views/index.scala.html", it compiles it to a class `views.html.index` that can be accessed by Play, and can contain rendering logic.

```
@(posts: Iterable[post.Post])(implicit request: post.PostRequest[_])

@main("Posts") {

  <div class="header">
    <h1 class="header-title">Posts</h1>
  </div>

  <table class="table table-striped">
    <thead>
      <td>Link</td>
      <td>Title</td>
      <td>Body</td>
      <td>Comments</td>
    </thead>
    <tbody>
    @for(post <- posts) {
      @row(post)
    }
    </tbody>
  </table>

}
```

There's not much that needs to be said about this.  One part that should be pointed out is that `(implicit request: post.PostRequest[_])` is passed in through the templates so that every template can leverage request information as needed.

## Post Action

We've discussed how the logic in the router works, but it's important to note that there is a `PostAction` that is working under the hood for every request, regardless of the application logic.  Defining a custom action for your project is not required, but it can be very handy.  In this example, we configure the PostAction with [scala-metrics](https://github.com/erikvanoosten/metrics-scala) and [SLF4J logging](http://www.slf4j.org/), append HTTP cache directives, and introduce a `PostRequest`.

``` scala
class PostAction @Inject()(config: PostActionConfig,
                           messagesApi: MessagesApi,
                           val metricRegistry: MetricRegistry)(implicit ec: ExecutionContext)
  extends ActionBuilder[PostRequest] with InstrumentedBuilder {

  type PostRequestBlock[A] = (PostRequest[A]) => Future[Result]

  private val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)

  private val requestsMeter = metrics.meter("requests-meter", "requests")

  private val wallClockTimer = metrics.timer("wall-clock")

  private val maxAge = config.maxAge.getSeconds

  override def invokeBlock[A](request: Request[A], block: PostRequestBlock[A]): Future[Result] = {
    if (logger.isTraceEnabled) {
      logger.trace(s"invokeBlock: request = $request")
    }

    // Count the number of requests
    requestsMeter.mark()

    // Measures the wall clock time to execute the action
    wallClockTimer.timeFuture {
      val messages = messagesApi.preferred(request)
      val postRequest = new PostRequest(request, messages)
      block(postRequest).map { result =>
        if (postRequest.method == "GET") {
          result.withHeaders(("Cache-Control", s"max-age: $maxAge"))
        } else {
          result
        }
      }
    }
  }
}
```

The configuration is provided using `PostActionConfig` which is a DTO containing a `java.time.Duration` instance, using [Typesafe Config](https://github.com/typesafehub/config):

``` scala
final case class PostActionConfig(maxAge: Duration)

object PostActionConfig {
  def fromConfiguration(config: Config): PostActionConfig = {
    val d = config.getDuration("restapi.postAction.maxAge")
    PostActionConfig(d)
  }
}
```

The `PostRequest` itself is an instance of `play.api.mvc.WrappedRequest`.  This is commonly used to hold request-specific information like security credentials, useful shortcut methods, and computed results that happened earlier up the chain.  Here, `PostRequest` contains a `Messages` instance that is mapped to the preferred language in the request -- this is especially useful in HTML templates, as form helpers all take an implicit `Messages` object.  We'll also add in a couple of methods that help with HTML rendering.

``` scala
class PostRequest[A](request: Request[A], val messages: Messages)
  extends WrappedRequest(request) {
    def flashSuccess: Option[String] = request.flash.get("success")
    def flashError: Option[String] = request.flash.get("error")
}
```

## Looking at configuration

Finally, there's the configuration and the dependency injection that breathes everything into life.  

The [configuration file](https://www.playframework.com/documentation/2.5.x/ConfigFile) for Play is `application.conf`, which is in [HOCON format](https://github.com/typesafehub/config/blob/master/HOCON.md).

Remember when we were talking about configuring a custom ExecutionContext for the repositories using [Akka Dispatcher](http://doc.akka.io/docs/akka/current/scala/dispatchers.html)?  Here's how that happens:

```
restapi {
  postAction.maxAge = 5 minutes

  postRepository {
    dispatcher {
      executor = "thread-pool-executor"
      throughput = 1
      thread-pool-executor {
        fixed-pool-size = 2  // sized to post repository db conn pool
      }
    }
  }

  commentRepository {
    dispatcher {
      executor = "thread-pool-executor"
      throughput = 1
      thread-pool-executor {
        fixed-pool-size = 5 // sized to comment repository db conn pool
      }
    }
  }
}
```

A few lines of configuration is all it takes.  The exact configuration of the thread pool is depends on the backing technology, but as a general rule, if you are using JDBC or a similar blocking model then you will need a ThreadPoolExecutor with a fixed size equal to the maximum number of JDBC connections in the JDBC connection pool (i.e. HikariCP).  You can read more about this in Play's [Thread Pool best practices](https://www.playframework.com/documentation/2.5.x/ThreadPools#best-practices) section. 

## Dependency Injection

Finally, Play provides [dependency injection](https://www.playframework.com/documentation/2.5.x/ScalaDependencyInjection) for the application when it starts up.  By default, dependency injection is done through [Guice](https://github.com/google/guice/wiki/GettingStarted), which uses an `AbstractModule` to bind interfaces with implementations:
 

``` scala
class Module(environment: Environment,
             configuration: Configuration) extends AbstractModule {
  override def configure() = {
    // Set up the HSTS filter configuration
    bind(classOf[StrictTransportSecurityConfig]).toProvider(classOf[StrictTransportSecurityConfigProvider])

    // Set up the two external datastores and their execution contexts
    bind(classOf[CommentExecutionContext]).toProvider(classOf[CommentExecutionContextProvider])
    bind(classOf[CommentRepository]).toProvider(classOf[CommentRepositoryProvider])
    bind(classOf[PostExecutionContext]).toProvider(classOf[PostExecutionContextProvider])
    bind(classOf[PostRepository]).toProvider(classOf[PostRepositoryProvider])

    // Set up the configuration for the PostAction
    bind(classOf[PostActionConfig]).toProvider(classOf[PostActionConfigProvider])

    // Hook in the coda hale metrics classes
    bind(classOf[MetricRegistry]).toProvider(classOf[MetricRegistryProvider])
    bind(classOf[MetricReporter]).asEagerSingleton()
  }
}
``` 
 
Here, most of the bindings are using `javax.inject.Provider`, which is a good technique for avoiding [cyclic dependencies](https://github.com/google/guice/wiki/CyclicDependencies) and deferring (or "lazy loading") injection until the Application is loaded.  (The exception here is the `MetricReporter`, which is not referenced by any Play component, and so must be started as an eager singleton by hand.)

Let's look at the `PostActionConfigProvider` as an example -- it binds the configuration object `PostActionConfig`  with the settings in `application.conf` ("5 minutes") by connecting it with the `play.api.Configuration` instance as follows:

```
@Singleton
class PostActionConfigProvider @Inject()(configuration: Configuration)
  extends Provider[PostActionConfig] {

  lazy val get: PostActionConfig = {
    PostActionConfig.fromConfiguration(configuration.underlying)
  }
}
```

## Conclusion

This concludes our quick tour of using Play as a REST API -- I hope this shows a good overview of Play's feature set.  Play makes it easy to write non-blocking code that makes the best use of system resources, and does so with fewer lines of code and a clean, expressive style.

Note that there's more involved in a REST API that there isn't room to cover here, such as pagination, filtering, sorting & searching.  For a more in-depth look, Adrian Hurt's seed template [play-api-rest-seed](https://github.com/adrianhurt/play-api-rest-seed), and look at [Silhouette](http://silhouette.mohiva.com/) and [Deadbolt 2](http://deadbolt.ws/) for authentication and authorization.
)



