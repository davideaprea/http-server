import model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import reader.dto.SizeLimits;
import router.Router;
import router.dto.HandlerCreateCommand;
import server.Server;
import server.ServerConfiguration;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ServerTest {

    private static final int DEFAULT_SOCKET_TIMEOUT_MILLIS = 5_000;
    private static final SizeLimits DEFAULT_LIMITS = new SizeLimits(16_384, 1_048_576);

    private Server server;
    private Thread serverThread;
    private int port;
    private final AtomicReference<Throwable> serverFailure = new AtomicReference<>();

    @AfterEach
    void tearDown() throws Exception {
        if (server != null) {
            server.stop();
        }

        if (serverThread != null) {
            serverThread.join(DEFAULT_SOCKET_TIMEOUT_MILLIS);
            assertFalse(serverThread.isAlive(), "Server thread did not stop.");
        }
    }

    // -------------------------------------------------------------------------
    // Basic HTTP behaviour - java.net.http.HttpClient
    // -------------------------------------------------------------------------

    @Test
    void getReturns200AndBody() throws Exception {
        startServer(routerForText(
                Method.GET,
                "/resource/path",
                "Hello world",
                true
        ));

        HttpResponse<String> response = send("GET", "/resource/path", HttpRequest.BodyPublishers.noBody());

        assertEquals(200, response.statusCode());
        assertEquals("Hello world", response.body());
        assertEquals("text/plain", response.headers().firstValue("content-type").orElseThrow());
        assertEquals("11", response.headers().firstValue("content-length").orElseThrow());
    }

    @Test
    void notFoundReturns404() throws Exception {
        startServer(routerForText(
                Method.GET,
                "/resource/path",
                "Hello world",
                true
        ));

        HttpResponse<String> response = send("GET", "/does-not-exist", HttpRequest.BodyPublishers.noBody());

        assertEquals(404, response.statusCode());
        assertEquals("Couldn't find the requested path.", response.body());
    }

    @Test
    void unsupportedMethodReturns405() throws Exception {
        startServer(routerForText(
                Method.GET,
                "/resource/path",
                "Hello world",
                true
        ));

        HttpRequest request = HttpRequest.newBuilder(uri("/resource/path"))
                .method("POST", HttpRequest.BodyPublishers.noBody())
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(Duration.ofSeconds(5))
                .build();

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(405, response.statusCode());
            assertEquals(
                    "The requested path is not configured for this method.",
                    response.body()
            );
        }
    }

    @Test
    void handlerExceptionReturns500() throws Exception {
        Router router = new Router.Builder()
                .add(new HandlerCreateCommand(
                        request -> {
                            throw new IllegalStateException("boom");
                        },
                        Method.GET,
                        "/failure"
                ))
                .build();

        startServer(router);

        HttpResponse<String> response = send("GET", "/failure", HttpRequest.BodyPublishers.noBody());

        assertEquals(500, response.statusCode());
        assertEquals("boom", response.body());
    }

    /*@ParameterizedTest
    @EnumSource(value = Method.class, names = {"GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"})
    void supportedMethodsReachTheirHandler(Method method) throws Exception {
        Router router = new Router.Builder()
                .add(new HandlerCreateCommand(
                        request -> textResponse(request.getMethod().name()),
                        method,
                        "/method"
                ))
                .build();

        startServer(router);

        HttpRequest.BodyPublisher publisher = method == Method.POST || method == Method.PUT || method == Method.PATCH
                ? HttpRequest.BodyPublishers.ofString("payload")
                : HttpRequest.BodyPublishers.noBody();

        HttpRequest request = HttpRequest.newBuilder(uri("/method"))
                .method(method.name(), publisher)
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(Duration.ofSeconds(5))
                .build();

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            assertEquals(method.name(), response.body());
        }
    }*/

    @Test
    void queryParametersAreDecodedAndRepeatedValuesArePreserved() throws Exception {
        Router router = new Router.Builder()
                .add(new HandlerCreateCommand(
                        request -> {
                            List<String> ids = request.getQueryParams().get("id");
                            String q = request.getQueryParams().get("q").getFirst();
                            return textResponse(q + "|" + String.join(",", ids));
                        },
                        Method.GET,
                        "/search"
                ))
                .build();

        startServer(router);

        HttpResponse<String> response = send(
                "GET",
                "/search?q=hello%20world&id=1&id=2",
                HttpRequest.BodyPublishers.noBody()
        );

        assertEquals(200, response.statusCode());
        assertEquals("hello world|1,2", response.body());
    }

    // -------------------------------------------------------------------------
    // Request body
    // -------------------------------------------------------------------------

    @Test
    void contentLengthRequestBodyIsReadableUntilEof() throws Exception {
        AtomicReference<byte[]> received = new AtomicReference<>();

        startServer(routerForBodyConsumer(
                Method.POST,
                "/echo-request",
                request -> {
                    received.set(readBody(request));
                    return textResponse("ok");
                },
                true
        ));

        byte[] payload = "request-body".getBytes(StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder(uri("/echo-request"))
                .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                .header("content-type", "application/octet-stream")
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(Duration.ofSeconds(5))
                .build();

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            assertEquals("ok", response.body());
        }

        assertArrayEquals(payload, received.get());
    }

    @Test
    void contentLengthZeroProducesImmediateEof() throws Exception {
        AtomicReference<Integer> firstRead = new AtomicReference<>();

        startServer(routerForBodyConsumer(
                Method.POST,
                "/empty-body",
                request -> {
                    firstRead.set(request.getBody().dequeue());
                    return textResponse("ok");
                },
                true
        ));

        HttpResponse<String> response = send(
                "POST",
                "/empty-body",
                HttpRequest.BodyPublishers.noBody()
        );

        assertEquals(200, response.statusCode());
        assertEquals("ok", response.body());
        assertEquals(-1, firstRead.get());
    }

    @Test
    void binaryRequestBodyPreservesByteValuesAbove127() throws Exception {
        AtomicReference<byte[]> received = new AtomicReference<>();

        startServer(routerForBodyConsumer(
                Method.POST,
                "/binary",
                request -> {
                    received.set(readBody(request));
                    return textResponse("ok");
                },
                true
        ));

        byte[] payload = {
                0x00, 0x01, 0x7F,
                (byte) 0x80, (byte) 0xFE, (byte) 0xFF
        };

        HttpRequest request = HttpRequest.newBuilder(uri("/binary"))
                .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(Duration.ofSeconds(5))
                .build();

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
        }

        assertArrayEquals(payload, received.get());
    }

    @Test
    void unknownLengthHttpClientBodyPublisherUsesChunkedRequestFraming() throws Exception {
        AtomicReference<byte[]> received = new AtomicReference<>();

        startServer(routerForBodyConsumer(
                Method.POST,
                "/chunked-http-client",
                request -> {
                    received.set(readBody(request));
                    return textResponse("ok");
                },
                true
        ));

        byte[] payload = "chunked through HttpClient".getBytes(StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder(uri("/chunked-http-client"))
                .POST(HttpRequest.BodyPublishers.ofInputStream(() -> new ByteArrayInputStream(payload)))
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(Duration.ofSeconds(5))
                .build();

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            assertEquals("ok", response.body());
        }

        assertArrayEquals(payload, received.get());
    }

    @Test
    void chunkedRequestBodyIsDecodedCorrectly() throws Exception {
        AtomicReference<byte[]> received = new AtomicReference<>();

        startServer(routerForBodyConsumer(
                Method.POST,
                "/chunked-request",
                request -> {
                    received.set(readBody(request));
                    return textResponse("ok");
                },
                true
        ));

        try (RawHttpConnection raw = newRawConnection()) {
            raw.send(
                    """
                            POST /chunked-request HTTP/1.1\r
                            Host: localhost\r
                            Transfer-Encoding: chunked\r
                            \r
                            5\r
                            hello\r
                            6\r
                             world\r
                            0\r
                            \r
                            """
            );

            RawResponse response = raw.readResponse(false);
            assertEquals(200, response.statusCode());
            assertEquals("ok", new String(response.body(), StandardCharsets.UTF_8));
        }

        assertArrayEquals("hello world".getBytes(StandardCharsets.UTF_8), received.get());
    }

    @Test
    @Timeout(value = 100, unit = TimeUnit.SECONDS)
    void largeRequestBodyCanFillTheBoundedQueueAndThenResumeReading() throws Exception {
        int bodySize = 128 * 1024;
        byte[] payload = new byte[bodySize];
        for (int i = 0; i < payload.length; i++) {
            payload[i] = (byte) (i * 31);
        }

        CountDownLatch firstByteRead = new CountDownLatch(1);
        CountDownLatch releaseBodyReader = new CountDownLatch(1);
        AtomicInteger bytesConsumed = new AtomicInteger();

        startServer(routerForBodyConsumer(
                Method.POST,
                "/backpressure",
                request -> {
                    int first = request.getBody().dequeue();
                    assertTrue(first >= 0 && first <= 255);
                    bytesConsumed.incrementAndGet();
                    firstByteRead.countDown();

                    assertTrue(releaseBodyReader.await(5, TimeUnit.SECONDS));

                    int value;
                    while ((value = request.getBody().dequeue()) != -1) {
                        bytesConsumed.incrementAndGet();
                    }

                    return textResponse("consumed=" + bytesConsumed.get());
                },
                true
        ));

        try (RawHttpConnection raw = newRawConnection()) {
            raw.sendHeaders(
                    "POST /backpressure HTTP/1.1\r\n" +
                            "Host: localhost\r\n" +
                            "Content-Length: " + payload.length + "\r\n" +
                            "\r\n"
            );

            Thread sender = new Thread(() -> {
                try {
                    raw.sendBytes(payload);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            sender.start();

            assertTrue(firstByteRead.await(5, TimeUnit.SECONDS));
            Thread.sleep(100);
            releaseBodyReader.countDown();

            sender.join(5_000);
            assertFalse(sender.isAlive(), "Request sender did not finish after backpressure was released.");

            RawResponse response = raw.readResponse(false);
            assertEquals(200, response.statusCode());
            assertEquals("consumed=" + bodySize, new String(response.body(), StandardCharsets.UTF_8));
        }
    }

    // -------------------------------------------------------------------------
    // Request validation / malformed input
    // -------------------------------------------------------------------------


    @Test
    void requestHeaderNamesAreCaseInsensitive() throws Exception {
        AtomicReference<byte[]> received = new AtomicReference<>();

        startServer(routerForBodyConsumer(
                Method.POST,
                "/header-case",
                request -> {
                    received.set(readBody(request));
                    return textResponse("ok");
                },
                true
        ));

        try (RawHttpConnection raw = newRawConnection()) {
            raw.send(
                    """
                            POST /header-case HTTP/1.1\r
                            hOsT: localhost\r
                            CoNtEnT-LeNgTh: 3\r
                            \r
                            abc"""
            );

            RawResponse response = raw.readResponse(false);
            assertEquals(200, response.statusCode());
            assertEquals("ok", new String(response.body(), StandardCharsets.UTF_8));
        }

        assertArrayEquals("abc".getBytes(StandardCharsets.UTF_8), received.get());
    }

    @Test
    void malformedRequestLineReturns400() throws Exception {
        startServer(routerForText(Method.GET, "/", "ok", true));

        try (RawHttpConnection raw = newRawConnection()) {
            raw.send("GET / HTTP/1.1\n\n");
            RawResponse response = raw.readResponse(false);

            assertEquals(400, response.statusCode());
            assertEquals("close", response.header("connection"));
        }
    }

    @Test
    void malformedHeaderReturns400() throws Exception {
        startServer(routerForText(Method.GET, "/", "ok", true));

        try (RawHttpConnection raw = newRawConnection()) {
            raw.send("GET / HTTP/1.1\r\nHost: localhost\r\nBrokenHeader\r\n\r\n");
            RawResponse response = raw.readResponse(false);

            assertEquals(400, response.statusCode());
            assertEquals("close", response.header("connection"));
        }
    }

    @Test
    void invalidContentLengthReturns400() throws Exception {
        startServer(routerForText(Method.GET, "/", "ok", true));

        try (RawHttpConnection raw = newRawConnection()) {
            raw.send(
                    """
                            POST / HTTP/1.1\r
                            Host: localhost\r
                            Content-Length: nope\r
                            \r
                            """
            );

            RawResponse response = raw.readResponse(false);
            assertEquals(400, response.statusCode());
        }
    }

    @Test
    void negativeContentLengthReturns400() throws Exception {
        startServer(routerForText(Method.GET, "/", "ok", true));

        try (RawHttpConnection raw = newRawConnection()) {
            raw.send(
                    """
                            POST / HTTP/1.1\r
                            Host: localhost\r
                            Content-Length: -1\r
                            \r
                            """
            );

            RawResponse response = raw.readResponse(false);
            assertEquals(400, response.statusCode());
        }
    }

    @Test
    void duplicateContentLengthReturns400() throws Exception {
        startServer(routerForText(Method.GET, "/", "ok", true));

        try (RawHttpConnection raw = newRawConnection()) {
            raw.send(
                    """
                            POST / HTTP/1.1\r
                            Host: localhost\r
                            Content-Length: 1\r
                            Content-Length: 1\r
                            \r
                            x"""
            );

            RawResponse response = raw.readResponse(false);
            assertEquals(400, response.statusCode());
        }
    }

    @Test
    void contentLengthAndTransferEncodingTogetherReturn400() throws Exception {
        startServer(routerForText(Method.GET, "/", "ok", true));

        try (RawHttpConnection raw = newRawConnection()) {
            raw.send(
                    """
                            POST / HTTP/1.1\r
                            Host: localhost\r
                            Content-Length: 1\r
                            Transfer-Encoding: chunked\r
                            \r
                            """
            );

            RawResponse response = raw.readResponse(false);
            assertEquals(400, response.statusCode());
        }
    }

    @Test
    void requestLineAndHeadersCannotExceedConfiguredLimit() throws Exception {
        startServer(
                routerForText(Method.GET, "/", "ok", true),
                16_384,
                8_000,
                new SizeLimits(10, 1_024)
        );

        try (RawHttpConnection raw = newRawConnection()) {
            raw.send("GET / HTTP/1.1\r\nHost: localhost\r\n\r\n");
            RawResponse response = raw.readResponse(false);
            String responseBody = new String(response.body(), StandardCharsets.UTF_8);
            assertEquals(400, response.statusCode());
            assertTrue(responseBody.contains("max size"));
        }
    }

    @Test
    void contentLengthExactlyAtMaxBodySizeIsAccepted() throws Exception {
        AtomicReference<byte[]> received = new AtomicReference<>();

        startServer(routerForBodyConsumer(
                Method.POST,
                "/body-limit-exact",
                request -> {
                    received.set(readBody(request));
                    return textResponse("ok");
                },
                true
        ), 4, 8, new SizeLimits(16_384, 4));

        byte[] payload = new byte[]{0, 1, 2, 3};
        HttpRequest request = HttpRequest.newBuilder(uri("/body-limit-exact"))
                .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(Duration.ofSeconds(5))
                .build();

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
        }

        assertArrayEquals(payload, received.get());
    }

    @Test
    void declaredContentLengthAboveMaxBodySizeClosesConnection() throws Exception {
        AtomicInteger invocations = new AtomicInteger();

        Router router = new Router.Builder()
                .add(new HandlerCreateCommand(
                        request -> {
                            invocations.incrementAndGet();
                            return textResponse("should-not-run");
                        },
                        Method.POST,
                        "/too-large"
                ))
                .build();

        startServer(router, 4, 8, new SizeLimits(16_384, 4));

        try (RawHttpConnection raw = newRawConnection()) {
            raw.send(
                    """
                            POST /too-large HTTP/1.1\r
                            Host: localhost\r
                            Content-Length: 5\r
                            \r
                            """
            );

            assertConnectionCloses(raw);
        }

        assertEquals(0, invocations.get());
    }

    @Test
    void chunkedBodyAboveMaxBodySizeClosesConnection() throws Exception {
        CountDownLatch handlerStarted = new CountDownLatch(1);

        Router router = new Router.Builder()
                .add(new HandlerCreateCommand(
                        request -> {
                            handlerStarted.countDown();
                            while (request.getBody().dequeue() != -1) {
                                // consume until the server aborts the connection
                            }
                            return textResponse("unexpected");
                        },
                        Method.POST,
                        "/chunk-too-large"
                ))
                .build();

        startServer(router, 4, 8, new SizeLimits(16_384, 4));

        try (RawHttpConnection raw = newRawConnection()) {
            raw.send(
                    """
                            POST /chunk-too-large HTTP/1.1\r
                            Host: localhost\r
                            Transfer-Encoding: chunked\r
                            \r
                            5\r
                            hello\r
                            0\r
                            \r
                            """
            );

            assertTrue(handlerStarted.await(2, TimeUnit.SECONDS));
            assertConnectionCloses(raw);
        }
    }

    @Test
    void malformedChunkSizeClosesConnection() throws Exception {
        startServer(routerForText(Method.POST, "/chunk", "ok", true));

        try (RawHttpConnection raw = newRawConnection()) {
            raw.send(
                    """
                            POST /chunk HTTP/1.1\r
                            Host: localhost\r
                            Transfer-Encoding: chunked\r
                            \r
                            not-hex\r
                            """
            );

            assertConnectionCloses(raw);
        }
    }

    // -------------------------------------------------------------------------
    // Timeout / lifecycle
    // -------------------------------------------------------------------------

    @Test
    void idleConnectionTimesOut() throws Exception {
        startServer(routerForText(Method.GET, "/", "ok", true), 4, 1, DEFAULT_LIMITS);

        try (RawHttpConnection raw = newRawConnection()) {
            assertConnectionCloses(raw, 2_000);
        }
    }

    @Test
    void incompleteRequestBodyTimesOut() throws Exception {
        CountDownLatch handlerStarted = new CountDownLatch(1);

        Router router = new Router.Builder()
                .add(new HandlerCreateCommand(
                        request -> {
                            handlerStarted.countDown();
                            while (request.getBody().dequeue() != -1) {
                                // wait for the request to complete
                            }
                            return textResponse("unexpected");
                        },
                        Method.POST,
                        "/timeout-body"
                ))
                .build();

        startServer(router, 2, 1, DEFAULT_LIMITS);

        try (RawHttpConnection raw = newRawConnection()) {
            raw.send(
                    """
                            POST /timeout-body HTTP/1.1\r
                            Host: localhost\r
                            Content-Length: 5\r
                            \r
                            a"""
            );

            assertTrue(handlerStarted.await(2, TimeUnit.SECONDS));
            assertConnectionCloses(raw, 2_000);
        }
    }

    @Test
    void malformedRequestDoesNotKillServerForOtherClients() throws Exception {
        startServer(routerForText(Method.GET, "/ok", "alive", true));

        try (RawHttpConnection malformed = newRawConnection()) {
            malformed.send("GET /ok HTTP/1.1\n\n");
            RawResponse error = malformed.readResponse(false);
            assertEquals(400, error.statusCode());
        }

        HttpResponse<String> response = send("GET", "/ok", HttpRequest.BodyPublishers.noBody());
        assertEquals(200, response.statusCode());
        assertEquals("alive", response.body());
    }

    @Test
    void stoppingServerStopsItsThread() throws Exception {
        startServer(routerForText(Method.GET, "/", "ok", true));

        server.stop();
        server = null;
        serverThread.join(2_000);

        assertFalse(serverThread.isAlive());
    }

    // -------------------------------------------------------------------------
    // Response framing / output path
    // -------------------------------------------------------------------------

    @Test
    void responseWithoutContentLengthUsesChunkedTransferEncoding() throws Exception {
        startServer(routerForText(Method.GET, "/chunked", "Hello chunked", false));

        try (RawHttpConnection raw = newRawConnection()) {
            raw.send("GET /chunked HTTP/1.1\r\nHost: localhost\r\n\r\n");
            RawResponse response = raw.readResponse(false);

            assertEquals(200, response.statusCode());
            assertEquals("chunked", response.header("transfer-encoding"));
            assertEquals("Hello chunked", new String(response.body(), StandardCharsets.UTF_8));
        }
    }


    @Test
    void largeChunkedResponseIsDeliveredIntactThroughHttpClient() throws Exception {
        byte[] expected = new byte[512 * 1024];
        for (int i = 0; i < expected.length; i++) {
            expected[i] = (byte) (i * 17);
        }

        startServer(new Router.Builder()
                .add(new HandlerCreateCommand(
                        request -> new Response(
                                Version.HTTP_1_1,
                                Status.OK,
                                Map.of(HeaderKey.CONTENT_TYPE.getValue(), "application/octet-stream"),
                                new ByteArrayInputStream(expected)
                        ),
                        Method.GET,
                        "/large-response"
                ))
                .build());

        HttpRequest request = HttpRequest.newBuilder(uri("/large-response"))
                .GET()
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(Duration.ofSeconds(10))
                .build();

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            assertEquals(200, response.statusCode());
            assertArrayEquals(expected, response.body());
            assertEquals("chunked", response.headers().firstValue("transfer-encoding").orElseThrow());
        }
    }


    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void slowRawClientStillReceivesLargeResponseAfterWriteWouldBlock() throws Exception {
        byte[] expected = new byte[2 * 1024 * 1024];
        for (int i = 0; i < expected.length; i++) {
            expected[i] = (byte) (i * 13);
        }

        startServer(new Router.Builder()
                .add(new HandlerCreateCommand(
                        request -> new Response(
                                Version.HTTP_1_1,
                                Status.OK,
                                Map.of(HeaderKey.CONTENT_TYPE.getValue(), "application/octet-stream"),
                                new ByteArrayInputStream(expected)
                        ),
                        Method.GET,
                        "/slow-reader"
                ))
                .build());

        try (RawHttpConnection raw = newRawConnection()) {
            raw.socket().setReceiveBufferSize(1024);
            raw.send("GET /slow-reader HTTP/1.1\r\nHost: localhost\r\n\r\n");

            // Do not read immediately: the server must tolerate a client that stops consuming data.
            Thread.sleep(250);

            RawResponse response = raw.readResponse(false);
            assertEquals(200, response.statusCode());
            assertArrayEquals(expected, response.body());
        }
    }

    @Test
    void responseWithContentLengthSendsExactlyDeclaredNumberOfBytes() throws Exception {
        Router router = new Router.Builder()
                .add(new HandlerCreateCommand(
                        request -> new Response(
                                Version.HTTP_1_1,
                                Status.OK,
                                Map.of(
                                        HeaderKey.CONTENT_TYPE.getValue(), "text/plain",
                                        HeaderKey.CONTENT_LENGTH.getValue(), "5"
                                ),
                                new ByteArrayInputStream("1234567890".getBytes(StandardCharsets.UTF_8))
                        ),
                        Method.GET,
                        "/too-long-response"
                ))
                .build();

        startServer(router);

        try (RawHttpConnection raw = newRawConnection()) {
            raw.send("GET /too-long-response HTTP/1.1\r\nHost: localhost\r\n\r\n");

            RawResponse response = raw.readResponse(false);
            assertEquals("12345", new String(response.body(), StandardCharsets.UTF_8));
            assertNoImmediateBytes(raw, 300);
        }
    }

    @Test
    void responseShorterThanContentLengthClosesBeforePretendingBodyIsComplete() throws Exception {
        Router router = new Router.Builder()
                .add(new HandlerCreateCommand(
                        request -> new Response(
                                Version.HTTP_1_1,
                                Status.OK,
                                Map.of(HeaderKey.CONTENT_LENGTH.getValue(), "10"),
                                new ByteArrayInputStream("12345".getBytes(StandardCharsets.UTF_8))
                        ),
                        Method.GET,
                        "/short-response"
                ))
                .build();

        startServer(router);

        try (RawHttpConnection raw = newRawConnection()) {
            raw.send("GET /short-response HTTP/1.1\r\nHost: localhost\r\n\r\n");

            assertThrows(EOFException.class, () -> raw.readResponse(false));
        }
    }

    // -------------------------------------------------------------------------
    // Connection persistence / pipelining / concurrency
    // -------------------------------------------------------------------------

    @Test
    void twoRequestsOnSameConnectionCanBeReadAndAnswered() throws Exception {
        startServer(new Router.Builder()
                .add(new HandlerCreateCommand(
                        request -> textResponse("one"),
                        Method.GET,
                        "/one"
                ))
                .add(new HandlerCreateCommand(
                        request -> textResponse("two"),
                        Method.GET,
                        "/two"
                ))
                .build());

        try (RawHttpConnection raw = newRawConnection()) {
            raw.send(
                    """
                            GET /one HTTP/1.1\r
                            Host: localhost\r
                            \r
                            GET /two HTTP/1.1\r
                            Host: localhost\r
                            \r
                            """
            );

            RawResponse first = raw.readResponse(false);
            RawResponse second = raw.readResponse(false);

            assertEquals(200, first.statusCode());
            assertEquals("one", new String(first.body(), StandardCharsets.UTF_8));
            assertEquals(200, second.statusCode());
            assertEquals("two", new String(second.body(), StandardCharsets.UTF_8));
        }
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void requestsOnSameConnectionAreProcessedSequentially() throws Exception {
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        AtomicInteger invocations = new AtomicInteger();

        startServer(new Router.Builder()
                .add(new HandlerCreateCommand(
                        request -> {
                            int current = invocations.incrementAndGet();
                            if (current == 1) {
                                firstStarted.countDown();
                                assertTrue(releaseFirst.await(5, TimeUnit.SECONDS));
                            }
                            return textResponse("response-" + current);
                        },
                        Method.GET,
                        "/one"
                ))
                .add(new HandlerCreateCommand(
                        request -> textResponse("response-" + invocations.incrementAndGet()),
                        Method.GET,
                        "/two"
                ))
                .build());

        try (RawHttpConnection raw = newRawConnection()) {
            raw.send(
                    """
                            GET /one HTTP/1.1\r
                            Host: localhost\r
                            \r
                            GET /two HTTP/1.1\r
                            Host: localhost\r
                            \r
                            """
            );

            assertTrue(firstStarted.await(2, TimeUnit.SECONDS));
            Thread.sleep(150);
            assertEquals(1, invocations.get(), "Second request was processed before first response completed.");

            releaseFirst.countDown();

            RawResponse first = raw.readResponse(false);
            RawResponse second = raw.readResponse(false);

            assertEquals("response-1", new String(first.body(), StandardCharsets.UTF_8));
            assertEquals("response-2", new String(second.body(), StandardCharsets.UTF_8));
        }
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void differentConnectionsCanBeProcessedConcurrently() throws Exception {
        CountDownLatch bothHandlersStarted = new CountDownLatch(2);
        CountDownLatch releaseHandlers = new CountDownLatch(1);

        Router router = new Router.Builder()
                .add(new HandlerCreateCommand(
                        request -> {
                            bothHandlersStarted.countDown();
                            assertTrue(releaseHandlers.await(5, TimeUnit.SECONDS));
                            return textResponse("ok");
                        },
                        Method.GET,
                        "/parallel"
                ))
                .build();

        startServer(router, 2, 8, DEFAULT_LIMITS);

        try (HttpClient firstClient = HttpClient.newHttpClient();
             HttpClient secondClient = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder(uri("/parallel"))
                    .GET()
                    .version(HttpClient.Version.HTTP_1_1)
                    .timeout(Duration.ofSeconds(10))
                    .build();

            var first = firstClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            var second = secondClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());

            assertTrue(bothHandlersStarted.await(3, TimeUnit.SECONDS));
            releaseHandlers.countDown();

            assertEquals(200, first.join().statusCode());
            assertEquals(200, second.join().statusCode());
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void startServer(Router router) throws Exception {
        startServer(router, 4, 8, DEFAULT_LIMITS);
    }

    private void startServer(Router router, int threadPoolSize, long requestTimeoutSeconds, SizeLimits limits) throws Exception {
        port = findFreePort();

        ServerConfiguration configuration = ServerConfiguration.builder()
                .port(port)
                .threadPoolSize(threadPoolSize)
                .requestTimeoutTime(requestTimeoutSeconds)
                .sizeLimits(limits)
                .router(router)
                .build();

        server = new Server(configuration);
        serverFailure.set(null);

        serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (Throwable t) {
                serverFailure.set(t);
            }
        }, "test-server");
        serverThread.setDaemon(true);
        serverThread.start();

        waitUntilListening();

        Throwable failure = serverFailure.get();
        if (failure != null) {
            fail("Server failed during startup", failure);
        }
    }

    private HttpResponse<String> send(String method, String path, HttpRequest.BodyPublisher bodyPublisher) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(Duration.ofSeconds(5));

        HttpRequest request = switch (method) {
            case "GET" -> builder.GET().build();
            case "POST" -> builder.POST(bodyPublisher).build();
            default -> builder.method(method, bodyPublisher).build();
        };

        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }

    private Router routerForText(Method method, String path, String body, boolean contentLength) {
        return new Router.Builder()
                .add(new HandlerCreateCommand(
                        request -> textResponse(body, contentLength),
                        method,
                        path
                ))
                .build();
    }

    private Router routerForBodyConsumer(
            Method method,
            String path,
            router.RequestHandler handler,
            boolean ignored
    ) {
        return new Router.Builder()
                .add(new HandlerCreateCommand(handler, method, path))
                .build();
    }

    private Response textResponse(String body) {
        return textResponse(body, true);
    }

    private Response textResponse(String body, boolean contentLength) {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(HeaderKey.CONTENT_TYPE.getValue(), "text/plain");
        if (contentLength) {
            headers.put(HeaderKey.CONTENT_LENGTH.getValue(), String.valueOf(bytes.length));
        }

        return new Response(
                Version.HTTP_1_1,
                Status.OK,
                headers,
                new ByteArrayInputStream(bytes)
        );
    }

    private byte[] readBody(Request request) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int value;

        while ((value = request.getBody().dequeue()) != -1) {
            output.write(value);
        }

        return output.toByteArray();
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }

    private RawHttpConnection newRawConnection() throws IOException {
        Socket socket = new Socket();
        socket.setTcpNoDelay(true);
        socket.setSoTimeout(5000);
        socket.connect(new InetSocketAddress("127.0.0.1", port), 5000);
        return new RawHttpConnection(socket);
    }

    private void waitUntilListening() throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        Throwable failure;

        do {
            failure = serverFailure.get();
            if (failure != null) {
                fail("Server failed during startup", failure);
            }

            try (Socket ignored = new Socket()) {
                ignored.connect(new InetSocketAddress("127.0.0.1", port), 100);
                return;
            } catch (IOException ignored) {
                Thread.sleep(20);
            }
        } while (System.nanoTime() < deadline);

        fail("Server did not start listening on port " + port);
    }

    private static int findFreePort() throws IOException {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }

    private static void assertConnectionCloses(RawHttpConnection raw) throws IOException {
        assertConnectionCloses(raw, 2_000);
    }

    private static void assertConnectionCloses(RawHttpConnection raw, int timeoutMillis) throws IOException {
        raw.socket().setSoTimeout(timeoutMillis);
        try {
            int value = raw.input().read();
            assertEquals(-1, value, "Expected the server to close the connection.");
        } catch (SocketTimeoutException e) {
            fail("Connection was not closed within " + timeoutMillis + " ms.");
        }
    }

    private static void assertNoImmediateBytes(RawHttpConnection raw, int timeoutMillis) throws IOException {
        raw.socket().setSoTimeout(timeoutMillis);
        try {
            int value = raw.input().read();

            if (value != -1) {
                fail("Unexpected bytes after the response: " + value);
            }
        } catch (SocketTimeoutException | EOFException expected) {
            // Expected: no body bytes, or the server closed the connection.
        }
    }

    private static final class RawHttpConnection implements AutoCloseable {
        private final Socket socket;
        private final java.io.BufferedInputStream input;
        private final OutputStream output;

        private RawHttpConnection(Socket socket) throws IOException {
            this.socket = socket;
            this.input = new java.io.BufferedInputStream(socket.getInputStream());
            this.output = socket.getOutputStream();
        }

        Socket socket() {
            return socket;
        }

        InputStream input() {
            return input;
        }

        void send(String request) throws IOException {
            sendBytes(request.getBytes(StandardCharsets.ISO_8859_1));
        }

        void sendHeaders(String requestHeaders) throws IOException {
            send(requestHeaders);
        }

        void sendBytes(byte[] bytes) throws IOException {
            output.write(bytes);
            output.flush();
        }

        RawResponse readResponse(boolean headRequest) throws IOException {
            String statusLine = readLine(input);
            if (statusLine == null) {
                throw new EOFException("Connection closed before response status line.");
            }

            String[] statusParts = statusLine.split(" ", 3);
            if (statusParts.length != 3) {
                throw new IOException("Invalid response status line: " + statusLine);
            }

            int status = Integer.parseInt(statusParts[1]);
            Map<String, String> headers = new LinkedHashMap<>();

            while (true) {
                String line = readLine(input);
                if (line == null) {
                    throw new EOFException("Connection closed while reading response headers.");
                }
                if (line.isEmpty()) {
                    break;
                }

                int separator = line.indexOf(':');
                if (separator <= 0) {
                    throw new IOException("Invalid response header: " + line);
                }

                String name = line.substring(0, separator).trim().toLowerCase();
                String value = line.substring(separator + 1).trim();
                headers.put(name, value);
            }

            if (headRequest) {
                return new RawResponse(status, headers, new byte[0]);
            }

            byte[] body;
            String contentLength = headers.get("content-length");
            String transferEncoding = headers.get("transfer-encoding");

            if (contentLength != null) {
                long length = Long.parseLong(contentLength);
                if (length > Integer.MAX_VALUE) {
                    throw new IOException("Test helper does not support huge Content-Length values.");
                }
                body = readExactly((int) length);
            } else if ("chunked".equalsIgnoreCase(transferEncoding)) {
                body = readChunkedBody();
            } else {
                body = readUntilEof();
            }

            return new RawResponse(status, headers, body);
        }

        private byte[] readExactly(int length) throws IOException {
            byte[] result = new byte[length];
            int offset = 0;

            while (offset < length) {
                int read = input.read(result, offset, length - offset);
                if (read == -1) {
                    throw new EOFException("Expected " + length + " bytes but received " + offset + ".");
                }
                offset += read;
            }

            return result;
        }

        private byte[] readChunkedBody() throws IOException {
            ByteArrayOutputStream result = new ByteArrayOutputStream();

            while (true) {
                String line = readLine(input);
                if (line == null) {
                    throw new EOFException("Connection closed inside chunked response.");
                }

                int semicolon = line.indexOf(';');
                String sizeText = semicolon >= 0 ? line.substring(0, semicolon) : line;
                long size = Long.parseLong(sizeText.trim(), 16);

                if (size == 0) {
                    // No trailers are expected from this server; consume the final empty line.
                    String trailerOrEmpty = readLine(input);
                    if (trailerOrEmpty == null) {
                        throw new EOFException("Connection closed after zero chunk.");
                    }
                    if (!trailerOrEmpty.isEmpty()) {
                        while (true) {
                            trailerOrEmpty = readLine(input);
                            if (trailerOrEmpty == null) {
                                throw new EOFException("Connection closed inside chunk trailers.");
                            }
                            if (trailerOrEmpty.isEmpty()) {
                                break;
                            }
                        }
                    }
                    return result.toByteArray();
                }

                if (size > Integer.MAX_VALUE) {
                    throw new IOException("Chunk too large for test helper.");
                }

                result.write(readExactly((int) size));

                String crlf = readLine(input);
                if (crlf == null || !crlf.isEmpty()) {
                    throw new IOException("Missing CRLF after chunk data.");
                }
            }
        }

        private byte[] readUntilEof() throws IOException {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;

            while ((read = input.read(buffer)) != -1) {
                result.write(buffer, 0, read);
            }

            return result.toByteArray();
        }

        private static String readLine(InputStream input) throws IOException {
            ByteArrayOutputStream line = new ByteArrayOutputStream();
            int previous = -1;

            while (true) {
                int current = input.read();
                if (current == -1) {
                    if (line.size() == 0) {
                        return null;
                    }
                    throw new EOFException("Unexpected EOF in line.");
                }

                if (previous == '\r' && current == '\n') {
                    byte[] bytes = line.toByteArray();
                    return new String(bytes, 0, bytes.length - 1, StandardCharsets.ISO_8859_1);
                }

                line.write(current);
                previous = current;
            }
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }

    private record RawResponse(
            int statusCode,
            Map<String, String> headers,
            byte[] body
    ) {
        String header(String name) {
            return headers.get(name.toLowerCase());
        }
    }
}
