# Java HTTP Server

A lightweight, dependency-free HTTP/1.1 server for Java.

## Features

- Non-blocking event loop: one selector thread handles all connections, a
  configurable worker pool runs request handlers
- Full HTTP/1.1 request parsing: request line, headers, and both body
  framing modes (`Content-Length` and `Transfer-Encoding: chunked`)
- Streaming request bodies — read the body as it arrives with
  `RequestBody.dequeue()`, without waiting for the full payload
- Backpressure in both directions: a slow handler pauses reading from the
  socket, a slow-reading client pauses writing of the response, so no single
  connection can force the server to buffer unbounded data in memory
- Keep-alive by default, with `Connection: close` honored on request or
  response
- Tree-based router, with `HEAD` automatically derived from every
  registered `GET`
- Per-server configurable size limits (header size, body size) and request
  timeout
- Unit and integration test suites — the integration suite drives the
  server over real sockets

## Requirements

- Java 21+
- [Lombok](https://projectlombok.org/) — used for builders and getters;
  enable annotation processing in your IDE

## Getting started

```java
Router router = new Router.Builder()
        .add(new HandlerCreateCommand(
                request -> Response.textResponse("Hello, World!", Status.OK),
                Method.GET,
                "/hello"
        ))
        .build();

Server server = new Server(ServerConfiguration.builder()
        .port(8080)
        .threadPoolSize(4)
        .router(router)
        .requestTimeoutTime(30) // seconds
        .sizeLimits(new SizeLimits(16_384, 1_048_576)) // max header size, max body size, in bytes
        .build());

// start() blocks the calling thread until stop() is called, so run it on its own thread
new Thread(() -> {
    try {
        server.start();
    } catch (IOException e) {
        throw new RuntimeException(e);
    }
}).start();

// later, from any other thread
server.stop();
```

```
curl http://localhost:8080/hello
```

## Routing

Routes are registered on a `Router.Builder` with a method, a path, and a
handler:

```java
Router router = new Router.Builder()
        .add(new HandlerCreateCommand(
                request -> Response.textResponse("Hello, World!", Status.OK),
                Method.GET,
                "/hello"
        ))
        .add(new HandlerCreateCommand(
                request -> createUser(request),
                Method.POST,
                "/users"
        ))
        .add(new HandlerCreateCommand(
                request -> getUser(request),
                Method.GET,
                "/users"
        ))
        .build();
```

Registering a `GET` handler for a path automatically registers a matching
`HEAD` handler alongside it, which runs the same logic and discards the
body. Registering the same method and path twice throws
`ConflictingRoutesException`.

A request that doesn't match any registered path gets a `404`; a path that
exists but doesn't support the requested method gets a `405`; if an exception
is thrown by the handler a `500` response is automatically returned.

## Handling requests

A `RequestHandler` receives a `Request` and returns a `Response`:

```java
request -> {
    String userAgent = request.getHeaderValue("user-agent").orElse("unknown");
    Map<String, List<String>> query = request.getQueryParams();

    return Response.textResponse("Hello, " + userAgent, Status.OK);
}
```

`Request` exposes:

| Method                    | Returns                                                            |
|---------------------------|---------------------------------------------------------------------|
| `getMethod()`             | `Method`                                                           |
| `getUrl()`                | `String` — the request path                                       |
| `getQueryParams()`        | `Map<String, List<String>>`                                       |
| `getHeaderValue(String)`  | `Optional<String>` — first value for that header (lowercase key)  |
| `getContentLength()`      | `Optional<Long>`                                                   |
| `isClosingRequest()`      | `boolean` — whether the client sent `Connection: close`           |
| `getBody()`               | `RequestBody`                                                      |

Header names are normalized to lowercase when parsed, so look them up in
lowercase, or use the constants on `HeaderKey` for the headers the server
itself knows about (`HOST`, `CONTENT_TYPE`, `CONTENT_LENGTH`,
`TRANSFER_ENCODING`, `CONNECTION`, `DATE`).

## Reading the request body

`RequestBody.dequeue()` returns the next byte of the body as an `int`,
blocking until one is available, and returns `-1` once the body has been
fully read:

```java
request -> {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    int b;

    while ((b = request.getBody().dequeue()) != -1) {
        buffer.write(b);
    }

    return Response.textResponse("Received " + buffer.size() + " bytes", Status.OK);
}
```

Because it's pulled byte by byte as it streams in, a handler can start
processing (parsing, writing to disk, forwarding elsewhere) before the
client has finished sending — there's no need to buffer the whole body
first.

## Building responses

`Response.textResponse(body, status)` covers plain text:

```java
Response.textResponse("Hello, World!", Status.OK);
```

For anything else — a different content type, a streaming body, custom
headers — build a `Response` directly:

```java
byte[] body = "{\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8);

Map<String, String> headers = new HashMap<>();
headers.put(HeaderKey.CONTENT_TYPE.getValue(), "application/json");
headers.put(HeaderKey.CONTENT_LENGTH.getValue(), String.valueOf(body.length));

new Response(Version.HTTP_1_1, Status.OK, headers, new ByteArrayInputStream(body));
```

If a response sets neither `Content-Length` nor `Transfer-Encoding`, the
router adds `Transfer-Encoding: chunked` automatically, so you don't need
to know the body length up front — useful when the body comes from a
stream rather than a byte array already in memory.

## Connection handling

Connections are kept alive by default across multiple requests. If the
client sends `Connection: close`, or a handler sets that header on its
response, the server closes the connection right after writing that
response.

## Configuration

`ServerConfiguration`, built via `ServerConfiguration.builder()`:

| Field                | Meaning                                                          |
|----------------------|-------------------------------------------------------------------|
| `port`               | TCP port to listen on                                             |
| `threadPoolSize`     | worker threads available to run request handlers                 |
| `router`             | the `Router` built with `Router.Builder`                          |
| `requestTimeoutTime` | seconds a connection may sit idle mid-request before being closed |
| `sizeLimits`         | `SizeLimits(maxHeadersSize, maxBodySize)`, in bytes                |

## Architecture

| Package  | Responsibility                                                                                          |
|----------|-----------------------------------------------------------------------------------------------------------|
| `server` | The selector event loop: accepts connections, dispatches readable/writable channels, wires everything together |
| `client` | Per-connection I/O: reading with backpressure, writing with backpressure, and a queue that serializes responses to pipelined requests |
| `reader` | The request-parsing lifecycle — a chain of small state-machine readers (request line → headers → body) that hand off to one another |
| `parser` | Stateless parsing helpers for the request line, targets, and header lines                                  |
| `model`  | `Request`, `Response`, and the other domain types                                                          |
| `router` | Path-to-handler tree, built with `Router.Builder`                                                          |
