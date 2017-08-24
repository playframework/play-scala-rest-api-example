# Play REST API

This is the example project for [Making a REST API in Play](http://developer.lightbend.com/guides/play-rest-api/index.html).

## Appendix

### Running

You need to download and install sbt for this application to run.

Once you have sbt installed, the following at the command prompt will start up Play in development mode:

```
sbt run
```

Play will start up on the HTTP port at http://localhost:9000/.   You don't need to deploy or reload anything -- changing any source code while the server is running will automatically recompile and hot-reload the application on the next HTTP request. 

### Usage

If you call the same URL from the command line, you’ll see JSON. Using httpie, we can execute the command:

```
http --verbose http://localhost:9000/v1/posts
```

and get back:

```
GET /v1/posts HTTP/1.1
```

Likewise, you can also send a POST directly as JSON:

```
http --verbose POST http://localhost:9000/v1/posts title="hello" body="world"
```

and get:

```
POST /v1/posts HTTP/1.1
```

### Load Testing

The best way to see what Play can do is to run a load test.  We've included Gatling in this test project for integrated load testing.

Start Play in production mode, by [staging the application](https://www.playframework.com/documentation/2.5.x/Deploying) and running the play script:s

```
sbt stage
cd target/universal/stage
bin/play-rest-api -Dplay.crypto.secret=testing
```

Then you'll start the Gatling load test up (it's already integrated into the project):

```
sbt gatling:test
```

For best results, start the gatling load test up on another machine so you do not have contending resources.  You can edit the [Gatling simulation](http://gatling.io/docs/2.2.2/general/simulation_structure.html#simulation-structure), and change the numbers as appropriate.

Once the test completes, you'll see an HTML file containing the load test chart:

```
 ./rest-api/target/gatling/gatlingspec-1472579540405/index.html
```

That will contain your load test results.

### Docker

In order to dockerize your play rest api example run the following steps:

1. Generate secret key - since docker will host our production app we need to generate a secret key.

```
sbt playGenerateSecret
```

copy the secret key (for example: "YtvWptxIlpf@Q[_dPvE[Z6NLAh_KU0YlGvwnN7sV8DKSk>PSBNbzarVp8j?=;l/y")

2. Create a new prod dist

```
sbt dist
```

3. Generate a new local Docker image

```
sbt docker:publishLocal
```

4. Run the play app inside a docker container

(* replace with your generated key)

```
docker run -p 9000:9000 -e APPLICATION_SECRET="YtvWptxIlpf@Q[_dPvE[Z6NLAh_KU0YlGvwnN7sV8DKSk>PSBNbzarVp8j?=;l/y" play-scala-rest-api-example:1.0-SNAPSHOT
```

5. Misc commands

to clean the image using sbt-native-packager

```
sbt docker:clean
```

* In order to stop all running docker containers
```
docker stop $(docker ps -aq)
```
