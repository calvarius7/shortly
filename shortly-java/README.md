# Shortly Backend

Spring Boot backend for the Shortly URL shortener. The API uses Redis for storage and exposes Actuator
endpoints for health checks and Prometheus metrics.

## Prerequisites

- JDK 25 (only for local build/run)
- Docker (for container image)
- Redis (local or via Docker/Kubernetes)

## Build the Docker image

Builds a native GraalVM image for the Shortly backend.
Since it's a native image, build time is much longer than a JVM image.

```bash
cd shortly-java
docker build -t shortly-backend:local .
```

## Run the container (with Redis)

```bash
# Start Redis locally
docker run --rm -p 6379:6379 --name shortly-redis redis:latest

# Start the backend (connects to Redis on the host)
docker run --rm -p 8080:8080 \
  -e SPRING_DATA_REDIS_HOST=host.docker.internal \
  shortly-backend:local
```

## Local run (without Docker, optional)

```bash
cd shortly-java
./mvnw -DskipTests package
./mvnw spring-boot:run
```

## Check health and metrics

```bash
curl -f http://localhost:8080/actuator/health
curl -f http://localhost:8080/actuator/prometheus | head
```

## Guides

The following guides illustrate how to use some features concretely:

* [Building a RESTful Web Service with Spring Boot Actuator](https://spring.io/guides/gs/actuator-service/)
* [Messaging with Redis](https://spring.io/guides/gs/messaging-redis/)
* [Getting Started with Sentry](https://docs.sentry.io/platforms/java/guides/spring-boot/)
* [Building a Reactive RESTful Web Service](https://spring.io/guides/gs/reactive-rest-service/)

### GraalVM Native Support

This project has been configured to let you generate either a lightweight container or a native executable.
It is also possible to run your tests in a native image.

### Lightweight Container with Cloud Native Buildpacks

If you're already familiar with Spring Boot container images support, this is the easiest way to get started.
Docker should be installed and configured on your machine prior to creating the image.

To create the image, run the following goal:

```
$ ./mvnw spring-boot:build-image -Pnative
```

Then, you can run the app like any other container:

```
$ docker run --rm shortly:0.0.1-SNAPSHOT
```

### Executable with Native Build Tools

Use this option if you want to explore more options such as running your tests in a native image.
The GraalVM `native-image` compiler should be installed and configured on your machine.

NOTE: GraalVM 25+ is required.

To create the executable, run the following goal:

```
$ ./mvnw native:compile -Pnative
```

Then, you can run the app as follows:

```
$ target/shortly
```

You can also run your existing tests suite in a native image.
This is an efficient way to validate the compatibility of your application.

To run your existing tests in a native image, run the following goal:

```
$ ./mvnw test -PnativeTest
```

### Testcontainers support

This project
uses [Testcontainers at development time](https://docs.spring.io/spring-boot/4.0.0/reference/features/dev-services.html#features.dev-services.testcontainers).

Testcontainers has been configured to use the following Docker images:

* [`redis:latest`](https://hub.docker.com/_/redis)

Please review the tags of the used images and set them to the same as you're running in production.
