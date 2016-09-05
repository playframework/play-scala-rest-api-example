# Making a REST API with Play

This guide will walk you through how to make a RESTful API with JSON using [Play 2.5](https://playframework.com).

We’ll demonstrate with a “best practices” REST API that you can clone from [http://github.com/playframework/play-rest-api](http://github.com/playframework/play-rest-api) -- this example is in Scala, but Play also has a Java API which looks and acts just like the Scala API. 

Note that there’s more involved in a REST API -- monitoring, representation, and managing access to back end resources -- that we'll cover in subsequent posts.  But first, let's address why Play is so effective as a REST API.

## Why use Play as a REST API?

Because Play is **built on reactive bedrock**.  Play starts from a reactive core, and builds on reactive principles all the way from the ground.

Play uses a small thread pool, and breaks network packets into a stream of small chunks of data keeps those threads fed with HTTP requests, which means it's fast.  and feeds those through Akka Streams, the Reactive Streams implementation designed by the people who invented Reactive Streams and wrote the Reactive Manifesto.  

In fact, Play is so fast that you have to turn off machines so that the rest of your architecture can keep up.  The Hootsuite team was able to **reduce the number of servers by 80%** by [switching to Play](https://www.lightbend.com/resources/case-studies-and-stories/how-hootsuite-modernized-its-url-shortener).  if you deploy Play with the same infrastructure that you were using for other web frameworks, you are effectively staging a denial of service attack against your own database.

Linkedin uses Play throughout its infrastructure. It wins on all [four quadrants of scalability](http://www.slideshare.net/brikis98/the-play-framework-at-linkedin/128-Outline1_Getting_started_with_Play2) ([video](https://youtu.be/8z3h4Uv9YbE)).  Play's average "request per second" comes in around [tens of k on a basic quad core w/o any intentional tuning](https://twitter.com/kevinbowling1/status/764188720140398592) -- and it only gets better.  

Play provides an easy to use MVC paradigm, including hot-reloading without any JVM bytecode magic or container overhead.  Startup time for a developer on Play was **reduced by roughly 7 times** for [Walmart Canada](https://www.lightbend.com/resources/case-studies-and-stories/walmart-boosts-conversions-by-20-with-lightbend-reactive-platform), and using Play **reduced development times by 2x to 3x**.

Play combines this with a **reactive programming API*** that lets you write async, non-blocking code in a straightforward fashion without worrying about complex and confusing "callback hell."

## Modelling a Post Resource

We'll start off with a REST API that displays information for blog posts.   This is a resource that will contain all the data to start with -- it will have a unique id, a URL hyperlink that indicates the canonical location of the resource, the title of the blog post, and the body of the blog post.

This resource is represented as a single case class in the Play application.

```scala
case class PostResource(id: String, link: String,
                        title: String, body: String)
```

This resource is mapped to and from JSON on the front end using Play, and is mapped to and from a persistent datastore on the backend using a handler.  

Play handles HTTP routing and representation for the REST API and makes it easy to write a non-blocking, asynchronous API that is an order of magnitude more efficient than other web application frameworks.  

## Routing Post Requests

Play has two complimentary routing mechanisms.  In the conf directory, there's a file called "routes" which contains entries for the HTTP method and a relative URL path, and points it at an action in a controller.

```
GET    /               controllers.HomeController.index()
```

This is useful for situations where a front end service is rendering HTML.  However, Play also contains a more powerful routing DSL that we will use for the REST API.

For every HTTP request starting with /posts, Play routes it to a dedicated PostRouter class to handle the Posts resource, through the conf/routes file: 

```
->     /posts               post.PostRouter
```

The PostRouter examines the URL and extracts data to pass along to the controller.

Play’s [routing DSL](https://www.playframework.com/documentation/2.5.x/ScalaSirdRouter) (aka SIRD) shows how data can be extracted from the URL concisely and cleanly:

```scala
package post
import javax.inject.Inject

import play.api.mvc._
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._

class PostRouter @Inject()(controller: PostController)
  extends SimpleRouter
{
  override def routes: Routes = {
    case GET(p"/") =>
     controller.index
      
    case POST(p"/") =>
      controller.process

    case GET(p"/$id") =>
      controller.show(id)      
  }
}
```

SIRD is based around HTTP methods and a string interpolated extractor object – this means that when we type the string “/$id” and prefix it with “p”, then the path parameter id can be extracted and used in the block. Naturally, there are also operators to extract queries, regular expressions, and even add custom extractors.  If you have a URL 

```
/posts/?sort=ascending&count=5
```

then you can extract the "sort" and "count" parameters in a single line:

```scala
GET("/" ? q_?"sort=$sort" & q_?”count=${ int(count) }")
```

Cake Solutions covers SIRD in more depth in a [fantastic blog post](http://www.cakesolutions.net/teamblogs/all-you-need-to-know-about-plays-routing-dsl).

## Using a Controller

The PostRouter has a PostController injected into it through standard [JSR-330 dependency injection](https://github.com/google/guice/wiki/JSR330).  A controller [handles the work of processing](https://www.playframework.com/documentation/2.5.x/ScalaActions)  the HTTP request into an HTTP response in the context of an Action: it's where page rendering and HTML form processing happen.  A controller extends [`play.api.mvc.Controller`](https://playframework.com/documentation/2.5.x/api/scala/index.html#play.api.mvc.Controller), which contains a number of utility methods and constants for working with HTTP.  In particular, a Controller contains Result objects such as Ok and Redirect, and HeaderNames like ACCEPT.

The methods in a controller consist of a method returning an [Action](https://playframework.com/documentation/2.5.x/api/scala/index.html#play.api.mvc.Action).  The Action provides the "engine" to Play.

Using the action, the controller passes in a block of code that takes a [`Request`](https://playframework.com/documentation/2.5.x/api/scala/index.html#play.api.mvc.Request) passed in as implicit – this means that any in-scope method that takes an implicit request as a parameter will use this request automatically.  Then, the block must return either a [`Result`](https://playframework.com/documentation/2.5.x/api/scala/index.html#play.api.mvc.Result), or a [`Future[Result]`](http://www.scala-lang.org/api/current/index.html#scala.concurrent.Future), depending on whether or not the action was called as "action { ... }" or [`action.async { ... }`](https://www.playframework.com/documentation/2.5.x/ScalaAsync#How-to-create-a-Future[Result]). 
 
### Handling GET Requests

Here's a simple example of a Controller:
 
```scala
import javax.inject.Inject
import play.api.mvc._

import scala.concurrent._

class MyController extends Controller {

  def index1: Action[AnyContent] = {
    Action { implicit request =>
      val r: Result = Ok("hello world")
      r
    }
  }

  def asyncIndex: Action[AnyContent] = {
    Action.async { implicit request =>
      val r: Future[Result] = Future.successful(Ok("hello world"))
      r
    }
  }
}
```

In this example, `index1` and `asyncIndex` have exactly the same behavior.  Internally, it makes no difference whether we call `Result` or `Future[Result]` -- Play is non-blocking all the way through. 

However, if you're already working with `Future`, async makes it easier to pass that `Future` around. You can read more about this in the [handling asynchronous results](https://www.playframework.com/documentation/2.5.x/ScalaAsync) section of the Play documentation.

Here's what the PostController methods dealing with GET requests looks like:

```scala
class PostController @Inject()(action: PostAction,
                              handler: PostResourceHandler)
                             (implicit ec: ExecutionContext)
 extends Controller {

 def index: Action[AnyContent] = {
   action.async { implicit request =>
     handler.find.map { posts =>
       Ok(Json.toJson(posts))
     }
   }
 }

 def show(id: String): Action[AnyContent] = {
   action.async { implicit request =>
     handler.lookup(id).map { post =>
       Ok(Json.toJson(post))
     }
   }
 }

}
```

Let's take `show` as an example.  Here, the action defines a workflow for a request that maps to a single resource, i.e. `GET /posts/123`.  

```scala
def show(id: String): Action[AnyContent] = {
  action.async { implicit request =>
    handler.lookup(id).map { post =>
      Ok(Json.toJson(post))
    }
  }
}
```

The id is passed in as a String, and the handler looks up and returns a `PostResource`.  The `Ok()` sends back a `Result` with a status code of "200 OK", containing a response body consisting of the `PostResource` serialized as JSON.

### Processing Form Input

Handling a POST request is also easy and is done through the `process` method:

```scala
class PostController @Inject()(action: PostAction,
                              handler: PostResourceHandler)
                             (implicit ec: ExecutionContext)
  extends Controller {

  private val form: Form[PostFormInput] = {
    import play.api.data.Forms._

    Form(
      mapping(
        "title" -> nonEmptyText,
        "body" -> text
      )(PostFormInput.apply)(PostFormInput.unapply)
    )
  }

  def process: Action[AnyContent] = {
    action.async { implicit request =>
      processJsonPost()
    }
  }

  private def processJsonPost[A]()(implicit request: PostRequest[A]):  Future[Result] = {
    def failure(badForm: Form[PostFormInput]) = {
      Future.successful(BadRequest(badForm.errorsAsJson))
    }

    def success(input: PostFormInput) = {
      handler.create(input).map { post =>
        Created(Json.toJson(post))
          .withHeaders(LOCATION -> post.link)
      }
    }

    form.bindFromRequest().fold(failure, success)
  }
}
```

Here, the `process` action is an action wrapper, and `processJsonPost` does most of the work.  In `processJsonPost`, we get to the [form processing](https://www.playframework.com/documentation/2.5.x/ScalaForms) part of the code.  

Here, `form.bindFromRequest()` will map input from the HTTP request to a [`play.api.data.Form`](https://www.playframework.com/documentation/2.5.x/api/scala/index.html#play.api.data.Form), and handles form validation and error reporting.  

If the `PostFormInput` passes validation, it's passed to the resource handler, using the `success` method.  If the form processing fails, then the `failure` method is called and the `FormError` is returned in JSON format.

```scala
private val form: Form[PostFormInput] = {
  import play.api.data.Forms._

  Form(
    mapping(
      "title" -> nonEmptyText,
      "body" -> text
    )(PostFormInput.apply)(PostFormInput.unapply)
  )
}
```

The form binds to the HTTP request using the names in the mapping -- "title" and "body" to the `PostFormInput` case class.

```scala
case class PostFormInput(title: String, body: String)
```

That's all you need to do to handle a basic web application!  As with most things, there are more details that need to be handled.  That's where creating custom Actions comes in.

## Using Actions

We saw in the `PostController` that each method is connected to an Action through the "action.async" method:

```scala
  def index: Action[AnyContent] = {
    action.async { implicit request =>
      handler.find.map { posts =>
        Ok(Json.toJson(posts))
      }
    }
  }
```

The action.async takes a function, and comes from the class parameter "action", which we can see is of type `PostAction`:

```scala
class PostController @Inject()(action: PostAction [...])
```

`PostAction` is an ActionBuilder.  It is involved in each action in the controller -- it mediates the paperwork involved with processing a request into a response, adding context to the request and enriching the response with headers and cookies.  ActionBuilders are essential for handling authentication, authorization and monitoring functionality.

ActionBuilders work through a process called [action composition](https://www.playframework.com/documentation/2.5.x/ScalaActionsComposition).  The ActionBuilder class has a method called `invokeBlock` that takes in a `Request` and a function (also known as a block, lambda or closure) that accepts a `Request` of a given type, and produces a `Future[Result]`.

So, if you want to work with an `Action` that has a "FooRequest" that has a Foo attached, it's easy: 

```scala
class FooRequest[A](request: Request[A], val foo: Foo) extends WrappedRequest(request)

class FooAction extends ActionBuilder[FooRequest] {
  type FooRequestBlock[A] = FooRequest[A] => Future[Result]

  override def invokeBlock[A](request: Request[A], block: FooRequestBlock[A]) = {
    block(new FooRequest[A](request, new Foo))
  }
}
```

You create an `ActionBuilder[FooRequest]`, override `invokeBlock`, and then call the function with an instance of `FooRequest`.  

Then, when you call `fooAction`, the request type is `FooRequest`:

```scala
fooAction { request: FooRequest => 
  Ok(request.foo.toString)
}
```

And `request.foo` will be added automatically.

You can keep composing action builders inside each other, so you don't have to layer all the functionality in one single ActionBuilder, or you can create a custom ActionBuilder for each package you work with, according to your taste.  For the purposes of this blog post, we'll keep everything together in a single class.

```scala
class PostRequest[A](request: Request[A], 
                     val messages: Messages)
  extends WrappedRequest(request)

class PostAction @Inject()(messagesApi: MessagesApi)
                          (implicit ec: ExecutionContext)
  extends ActionBuilder[PostRequest] with HttpVerbs {

  type PostRequestBlock[A] = PostRequest[A] => Future[Result]

  private val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)

  override def invokeBlock[A](request: Request[A], 
                              block: PostRequestBlock[A]) = {
    if (logger.isTraceEnabled()) {
      logger.trace(s"invokeBlock: request = $request")
    }

    val messages = messagesApi.preferred(request)
    val future = block(new PostRequest(request, messages))

    future.map { result =>
      request.method match {
        case GET | HEAD =>
          result.withHeaders("Cache-Control" -> s"max-age: 100")
        case other =>
          result
      }
    }
  }
}
```

`PostAction` does a couple of different things here.  The first thing it does is to log the request as it comes in.  Next, it pulls out the localized `Messages` for the request, and adds that to a `PostRequest` , and runs the function, returning a `Future[Result]`.

When the future completes, we map the result so we can replace it with a slightly different result.  We compare the result's method against `HttpVerbs`, and if it's a GET or HEAD, we append a Cache-Control header with a max-age directive.  We need an `ExecutionContext` for `future.map` operations, so we pass in the default execution context implicitly at the top of the class.

Now that we have a `PostRequest`, we can call "request.messages" explicitly from any action in the controller, for free, and we can append information to the result after the user action has been completed.

## Converting resources with PostResourceHandler

The `PostResourceHandler` is responsible for converting backend data from a repository into a `PostResource`. We won't go into detail on the `PostRepository` details for now, only that it returns data in an backend-centric state.

A REST resource has information that a backend repository does not -- it knows about the operations available on the resource, and contains URI information that a single backend may not have.  As such, we want to be able to change the representation that we use internally without changing the resource that we expose publicly.  

```scala
class PostResourceHandler @Inject()(routerProvider: Provider[PostRouter],
                                   postRepository: PostRepository)
                                  (implicit ec: ExecutionContext)
{

 def create(postInput: PostFormInput): Future[PostResource] = {
   val data = PostData(PostId("999"), postInput.title, postInput.body)
   postRepository.create(data).map { id =>
     createPostResource(data)
   }
 }

 def lookup(id: String): Future[Option[PostResource]] = {
   val postFuture = postRepository.get(PostId(id))
   postFuture.map { maybePostData =>
     maybePostData.map { postData =>
       createPostResource(postData)
     }
   }
 }

 def find: Future[Iterable[PostResource]] = {
   postRepository.list().map { postDataList =>
     postDataList.map(postData => createPostResource(postData))
   }
 }

 private def createPostResource(p: PostData): PostResource = {
   PostResource(p.id.toString, routerProvider.get.link(p.id), p.title, p.body)
 }

}
```

Here, it's a straight conversion in `createPostResource`, with the only hook being that the router provides the resource's URL, since it's something that `PostData` doesn't have itself.

## Rendering Content as JSON

Play handles the work of converting a `PostResource` through [Play JSON](https://www.playframework.com/documentation/2.5.x/ScalaJson). Play JSON provides a DSL that looks up the conversion for the PostResource singleton object:

```scala
object PostResource {
  implicit val implicitWrites = new Writes[PostResource] {
    def writes(post: PostResource): JsValue = {
      Json.obj(
        "id" -> post.id,
        "link" -> post.link,
        "title" -> post.title,
        "body" -> post.body)
    }
  }
}
```

Once the implicit is defined in the companion object, then it will be looked up automatically when handed an instance of the class.  This means that when the controller converts to JSON, the conversion will just work, without any additional imports or setup.  

```scala
val json: JsValue = Json.toJson(post)
```

Play JSON also has options to incrementally parse and generate JSON for continuously streaming JSON responses.

## Summary

We've shown how to easy it is to put together a scalable REST API in Play.  Using this code, we can put together backend data, convert it to JSON and transfer it over HTTP with a minimum of fuss.

From here, the sky is the limit.  

Check out the [Play tutorials](https://playframework.com/documentation/2.5.x/Tutorials) and see more examples and blog posts about Play, including streaming [Server Side Events](https://github.com/playframework/play-streaming-scala) and first class [WebSocket support](https://github.com/playframework/play-websocket-scala).

To get more involved and if you have questions, join the [mailing list](https://groups.google.com/forum/#!forum/play-framework) at  and follow [PlayFramework on Twitter](https://twitter.com/playframework).

## Appendix

### Running

You need to download and install sbt for this application to run.

Once you have sbt installed, the following at the command prompt will start up Play in development mode:

```
sbt run
```

Play will start up on the HTTP port at http://localhost:9000/.   You don't need to reploy or reload anything -- changing any source code while the server is running will automatically recompile and hot-reload the application on the next HTTP request. 

### Usage

If you call the same URL from the command line, you’ll see JSON. Using httpie, we can execute the command:

```
http --verbose http://localhost:9000/posts
```

and get back:

```
GET /posts HTTP/1.1
```

Likewise, you can also send a POST directly as JSON:

```
http --verbose POST http://localhost:9000/posts title="hello" body="world"
```

and get:

```
POST /posts HTTP/1.1
```

### Load Testing

The best way to see what Play can do is to run a load test.  We've included Gatling in this test project for integrated load testing.

Start Play in production mode, by staging the application and running the play script:

https://www.playframework.com/documentation/2.5.x/Deploying

```
sbt stage
cd target/universal/stage
bin/play-rest-api -Dplay.crypto.secret=testing
```

Then you'll start the Gatling load test up (it's already integrated into the project):

```
sbt gatling:test
```

For best results, start the gatling load test up on another machine so you do not have contending resources.  You can edit the Gatling simulation, and change the numbers as appropriate http://gatling.io/docs/2.2.2/general/simulation_structure.html#simulation-structure

Once the test completes, you'll see an HTML file containing the load test chart:

 ./rest-api/target/gatling/gatlingspec-1472579540405/index.html

That will contain your load test results.
