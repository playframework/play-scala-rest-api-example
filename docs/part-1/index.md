# Making a REST API with Play

This guide will walk you through how to make a RESTful API with JSON using Play 2.5.

We’ll demonstrate with a “best practices” REST API that you can clone from http://github.com/playframework/play-rest-api -- this example is in Scala, but Play also has a Java API which looks and acts just like the Scala API. 

Note that there’s more involved in a REST API -- monitoring, representation, and managing access to back end resources -- that we'll cover in subsequent posts.  But first, let's address why Play is so effective as a REST API.

## Why use Play as a REST API?

Because Play is built on reactive bedrock.  Play starts from a reactive core, and builds on reactive principles all the way from the ground.

Play uses a small thread pool, and keeps those threads fed with HTTP requests using the Reactor pattern, which means it's fast.  For streaming requests, it breaks network packets into a stream of small chunks of data and feeds those through Akka Streams, the Reactive Streams implementation designed by the people who invented Reactive Streams and wrote the Reactive Manifesto.  

In fact, Play is so fast that you have to turn off machines so that the rest of your architecture can keep up.  The Hootsuite team was able to reduce the number of servers by 80% by switching to Play.  if you deploy Play with the same infrastructure that you were using for other web frameworks, you are effectively staging a denial of service attack against your own database.

Linkedin uses Play throughout its infrastructure. It wins on all four quadrants of scalability.  (You can watch a video about it.)  Play's average "request per second" comes in around "tens of k on a basic quad core w/o any intentional tuning" -- and it only gets better.  

Play provides an easy to use MVC paradigm, including hot-reloading without any JVM bytecode magic or container overhead.  Startup time for a developer on Play was reduced by roughly 7 times for Walmart Canada, and using Play reduced development times by 2x to 3x.

Play combines this with a Reactive Programming API that lets you write async, non-blocking code in a straightforward fashion using Future and CompletableStage directly, without worrying about complex and confusing "callback hell."

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

Play’s routing DSL (aka SIRD) shows how data can be extracted from the URL concisely and cleanly:

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

Cake Solutions covers SIRD in more depth in a fantastic blog post.

## Controlling Representation with Controller

The PostRouter has a PostController injected into it through standard JSR-330 dependency injection.  A controller handles the work of processing the HTTP request into an HTTP response in the context of an Action: it's where page rendering and HTML form processing happen.  A controller typically extends play.api.mvc.Controller, which contains a number of utility methods and constants for working with HTTP.  In particular, a Controller contains Result objects such as Ok and Redirect, and HeaderNames like ACCEPT.

A controller produces an Action, which provides the "engine" to Play.  Using the action, the controller passes in a block of code that takes a request passed in as implicit – this means that any in-scope method that takes an implicit request as a parameter will use this request automatically.  Then, the block must return either a Result, or a Future[Result], depending on whether or not the action was called as "action { ... }" or "action.async { ... }".

```scala
import javax.inject.Inject
import play.api.mvc._

import scala.concurrent._

class MyController @Inject()(action: ActionBuilder[AnyContent]) extends Controller {

  def index1: Action[AnyContent] = {
    action { implicit request =>
      val r: Result = Ok("hello world")
      r
    }
  }

  def asyncIndex: Action[AnyContent] = {
    action.async { implicit request =>
      val r: Future[Result] = Future.successful(Ok("hello world"))
      r
    }
  }
}
```

In this example, index1 and asyncIndex have exactly the same behavior.  Internally, it makes no difference whether we call `Result` or `Future[Result]` --  Play is non-blocking all the way through. 

However, if you're already working with Future, async makes it easier to pass that Future around. You can read more about this in the handling asynchronous results section of the Play documentation.

Here's what PostController looks like:

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

 def index: Action[AnyContent] = {
   action.async { implicit request =>
     handler.find.map { posts =>
       Ok(Json.toJson(posts))
     }
   }
 }

 def process: Action[AnyContent] = {
   action.async { implicit request =>
     processJsonPost()
   }
 }

 def show(id: String): Action[AnyContent] = {
   action.async { implicit request =>
     handler.lookup(id).map { post =>
       Ok(Json.toJson(post))
     }
   }
 }

 private def processJsonPost[A]()(implicit request: PostRequest[A]): Future[Result] = {
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

Let's take show as an example.  Here, the action defines a workflow for a request that maps to a single resource, i.e. GET /posts/123.  

```scala
def show(id: String): Action[AnyContent] = {
  action.async { implicit request =>
    handler.lookup(id).map { post =>
      Ok(Json.toJson(post))
    }
  }
}
```

The id is passed in, and the handler looks up and returns a PostResource.  The Result sends back a 200 OK with the PostResource serialized as JSON.

Handling a POST request is also easy:

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

The form binds to the request using the names in the mapping -- "title" and "body" to the PostFormInput case class, and handles form validation and error reporting in processJsonPost.  If the PostFormInput passes validation, it's passed to the resource handler.

```scala
 private def processJsonPost[A]()(implicit request: PostRequest[A]): Future[Result] = {
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
```

Note the injected class parameters in the PostController:

```scala
class PostController @Inject()(handler: PostResourceHandler)
                               (implicit ec: ExecutionContext)
```

The PostController relies on dependency injection to provide it with a PostResourceHandler, which provides the PostResource to the controller and handles the mapping internally.

## Managing Actions with ActionBuilder

We saw in the PostController that each method is connected to an Action through the "action.async" method:

```scala
  def index: Action[AnyContent] = {
    action.async { implicit request =>
      handler.find.map { posts =>
        Ok(Json.toJson(posts))
      }
    }
  }
```

The action.async takes a function, and comes from the class parameter "action", which we can see is of type PostAction:

```scala
class PostController @Inject()(action: PostAction [...])
```

PostAction is involved in each action in the controller -- it mediates the paperwork involved with processing a request into a response, adding context to the request and enriching the response with headers and cookies.  ActionBuilders are essential for handling authentication, authorization and monitoring functionality.

ActionBuilders work through a process called function composition.   This is called "Action Composition" in the Play documentation.  The ActionBuilder class has a method called "invokeBlock" that takes in a request and a function (also known as a block, lambda or closure) that accepts a request of a given type, and produces a Future[Result].

So, if you want to work with an Action that has a "FooRequest", it's easy.  You extend WrappedRequest, create an `ActionBuilder[FooRequest]`, override invokeBlock, and then call the function with an instantiation of FooRequest: 

```scala
class FooRequest[A](request: Request[A]) extends WrappedRequest(request)

class FooAction extends ActionBuilder[FooRequest] {
  type FooRequestBlock[A] = FooRequest[A] => Future[Result]

  override def invokeBlock[A](request: Request[A], block: FooRequestBlock[A]) = {
    block(new FooRequest[A](request))
  }
}
```

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
          result.withHeaders(("Cache-Control", s"max-age: 100"))
        case other =>
          result
      }
    }
  }
}
```

PostAction does a couple of different things here.  The first thing it does is to log the request as it comes in with a custom marker, to allow for triggering and filtering log statements.  Next, it pulls out the localized Messages for the request, and adds that to a PostRequest instance, and runs the function, returning a Future[Result].

When the future completes, we map the result so we can replace it with a slightly different result.  We compare the result's method against HttpVerbs, and if it's a GET or HEAD, we append a Cache-Control header with a max-age directive.  We need an ExecutionContext for future.map operations, so we pass in the default execution context implicitly at the top of the class.

Now that we have a PostRequest, we can call "request.messages" explicitly from any action in the controller, for free, and we can append information to the result after the user action has been completed.

## Converting resources with PostResourceHandler

The PostResourceHandler is responsible for converting backend data from a repository into a PostResource. We won't go into detail on the PostRepository details for now, only that it returns data in an backend-centric state.

A REST resource has information that a backend repository does not -- it knows about the operations available on the resource, and contains URI information that a single backend may not have.  As such, we want to be able to change the representation that we use internally without changing the resource that we expose publically.  

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

Here, it's a straight conversion in createPostResource, with the only hook being that the router provides the resource's URL, since it's something that PostData doesn't have itself.

## Rendering Content as JSON

Play handles the work of converting a PostResource through Play JSON. Play JSON provides a DSL that looks up the conversion for the PostResource singleton object:

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

Once the implicit is defined in the companion object, then it will be looked up automatically when handed an instance of the class.  This means that when the controller converts to JSON,

```scala
val json: JsValue = Json.toJson(post)
```

will just work, without any additional imports or setup.  Play JSON also has options to incrementally parse and generate JSON for continuously streaming JSON responses.

## Summary

We've shown how to easy it is to put together a scalable REST API in Play.  Using this code, we can put together backend data, convert it to JSON and transfer it over HTTP with a minimum of fuss.

From here, the sky is the limit.  

Check out the Play tutorials at https://playframework.com/documentation/2.5.x/Tutorials and see more examples and blog posts about Play, including streaming Server Side Events https://github.com/playframework/play-streaming-scala and first class WebSocket support https://github.com/playframework/play-websocket-scala

To get more involved and if you have questions, join the mailing list at https://groups.google.com/forum/#!forum/play-framework and follow PlayFramework on Twitter https://twitter.com/playframework.

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
