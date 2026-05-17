package io.github.mundanej.mjo.iiop;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjo.cdr.CdrReader;
import io.github.mundanej.mjo.cdr.CdrWriter;
import io.github.mundanej.mjo.common.BoundedLimit;
import io.github.mundanej.mjo.giop.GiopCloseConnection;
import io.github.mundanej.mjo.giop.GiopHeader;
import io.github.mundanej.mjo.giop.GiopLimits;
import io.github.mundanej.mjo.giop.GiopMessageType;
import io.github.mundanej.mjo.giop.GiopMessageWriter;
import io.github.mundanej.mjo.giop.GiopReply;
import io.github.mundanej.mjo.giop.GiopReplyStatus;
import io.github.mundanej.mjo.giop.GiopRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit and loopback integration tests for the local IIOP TCP slice. */
@Tag("unit")
final class IiopTcpTest {

  @Test
  void generatedStyleHelloRequestReplySucceedsOverLoopbackTcp() {
    try (IiopServer server =
        IiopServer.bind(IiopEndpoint.loopback(0), IiopOptions.defaults(), this::handleHello)) {
      try (IiopClient client = IiopClient.connect(server.endpoint(), IiopOptions.defaults())) {
        assertEquals(server.endpoint(), client.endpoint());

        GiopReply reply = client.invoke(helloRequest(1, "Ada"));

        assertEquals(GiopReplyStatus.NO_EXCEPTION, reply.replyStatus());
        assertEquals("Hello, Ada", CdrReader.bigEndian(reply.body()).readString());
      }
    }
  }

  @Test
  void requestIdCorrelationMismatchClosesClientDeterministically() {
    try (IiopServer server =
        IiopServer.bind(
            IiopEndpoint.loopback(0),
            IiopOptions.defaults(),
            request -> reply(request.requestId() + 1, "wrong"))) {
      try (IiopClient client = IiopClient.connect(server.endpoint(), IiopOptions.defaults())) {
        assertIiopCode(
            IiopDiagnosticCodes.CORRELATION_FAILURE, () -> client.invoke(helloRequest(7, "Bob")));
        assertIiopCode(IiopDiagnosticCodes.LIFECYCLE, () -> client.invoke(helloRequest(8, "Bob")));
      }
    }
  }

  @Test
  void invalidGiopFramesMapToIiopFailureAndCloseClient() throws Exception {
    AtomicReference<Throwable> peerFailure = new AtomicReference<>();

    try (ServerSocket peer = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
      Thread peerThread =
          new Thread(
              () -> {
                try (Socket socket = peer.accept()) {
                  IiopFrameCodec.readMessage(socket.getInputStream(), GiopLimits.defaults());
                  socket
                      .getOutputStream()
                      .write(bytes(0x47, 0x49, 0x4F, 0x50, 0x01, 0x02, 0x04, 0x01, 0, 0, 0, 0));
                  socket.getOutputStream().flush();
                } catch (Throwable throwable) {
                  peerFailure.set(throwable);
                }
              },
              "mjo-iiop-invalid-frame-peer");
      peerThread.setDaemon(true);
      peerThread.start();

      try (IiopClient client =
          IiopClient.connect(IiopEndpoint.fromBoundSocket(peer), IiopOptions.defaults())) {
        assertIiopCode(
            IiopDiagnosticCodes.UNSUPPORTED_MESSAGE, () -> client.invoke(helloRequest(10, "Gia")));
        assertIiopCode(IiopDiagnosticCodes.LIFECYCLE, () -> client.invoke(helloRequest(11, "Gia")));
      }

      peerThread.join(2_000);
      assertTrue(!peerThread.isAlive());
      assertNull(peerFailure.get());
    }
  }

  @Test
  void connectAndReadTimeoutFailuresAreDeterministic() {
    IiopOptions timeoutOptions =
        new IiopOptions(Duration.ofMillis(25), Duration.ofMillis(25), 4, 4, GiopLimits.defaults());
    assertIiopCode(
        IiopDiagnosticCodes.CONNECT_TIMEOUT,
        () ->
            IiopClient.connect(
                IiopEndpoint.loopback(1),
                timeoutOptions,
                (endpoint, options) -> {
                  throw new SocketTimeoutException("synthetic connect timeout");
                }));

    CountDownLatch handlerEntered = new CountDownLatch(1);
    CountDownLatch releaseHandler = new CountDownLatch(1);
    try (IiopServer server =
        IiopServer.bind(
            IiopEndpoint.loopback(0),
            timeoutOptions,
            request -> {
              handlerEntered.countDown();
              if (!releaseHandler.await(1, TimeUnit.SECONDS)) {
                throw new IllegalStateException("test handler was not released");
              }
              return reply(request.requestId(), "late");
            })) {
      try (IiopClient client = IiopClient.connect(server.endpoint(), timeoutOptions)) {
        assertIiopCode(
            IiopDiagnosticCodes.READ_TIMEOUT, () -> client.invoke(helloRequest(2, "Cid")));
      } finally {
        releaseHandler.countDown();
      }
    }
    assertEquals(0, handlerEntered.getCount());
  }

  @Test
  void oversizedDeclaredFramesFailBeforeBodyAllocation() {
    GiopLimits strictLimits =
        new GiopLimits(
            new BoundedLimit("test-message", 12),
            new BoundedLimit("test-body", 0),
            new BoundedLimit("test-context-count", 1),
            new BoundedLimit("test-context-data", 1));
    byte[] headerDeclaringBody =
        bytes(0x47, 0x49, 0x4F, 0x50, 0x01, 0x02, 0x00, 0x05, 0x00, 0x00, 0x00, 0x01);

    assertIiopCode(
        IiopDiagnosticCodes.FRAME_LIMIT,
        () ->
            IiopFrameCodec.readMessage(
                new ByteArrayInputStream(headerDeclaringBody), strictLimits));
  }

  @Test
  void malformedFrameHeadersFailDeterministically() {
    assertIiopCode(
        IiopDiagnosticCodes.EOF,
        () -> IiopFrameCodec.readMessage(new ByteArrayInputStream(bytes()), GiopLimits.defaults()));
    assertIiopCode(
        IiopDiagnosticCodes.UNSUPPORTED_MESSAGE,
        () ->
            IiopFrameCodec.readMessage(
                new ByteArrayInputStream(
                    bytes(0x00, 0x49, 0x4F, 0x50, 0x01, 0x02, 0x00, 0x05, 0, 0, 0, 0)),
                GiopLimits.defaults()));

    GiopLimits strictLimits =
        new GiopLimits(
            new BoundedLimit("test-message", 13),
            new BoundedLimit("test-body", 0),
            new BoundedLimit("test-context-count", 1),
            new BoundedLimit("test-context-data", 1));
    byte[] littleEndianHeaderDeclaringBody =
        bytes(0x47, 0x49, 0x4F, 0x50, 0x01, 0x02, 0x01, 0x05, 0x01, 0x00, 0x00, 0x00);

    assertIiopCode(
        IiopDiagnosticCodes.FRAME_LIMIT,
        () ->
            IiopFrameCodec.readMessage(
                new ByteArrayInputStream(littleEndianHeaderDeclaringBody), strictLimits));
  }

  @Test
  void endpointOptionsAndExceptionInputsAreValidated() {
    assertIiopCode(IiopDiagnosticCodes.INVALID_CONFIGURATION, () -> new IiopEndpoint("", 1));
    assertIiopCode(IiopDiagnosticCodes.INVALID_CONFIGURATION, () -> IiopEndpoint.loopback(-1));
    assertIiopCode(IiopDiagnosticCodes.INVALID_CONFIGURATION, () -> IiopEndpoint.loopback(65_536));
    assertIiopCode(
        IiopDiagnosticCodes.INVALID_CONFIGURATION,
        () -> new IiopOptions(Duration.ofMillis(-1), Duration.ZERO, 1, 1, GiopLimits.defaults()));
    assertIiopCode(
        IiopDiagnosticCodes.INVALID_CONFIGURATION,
        () -> new IiopOptions(Duration.ZERO, Duration.ZERO, 0, 1, GiopLimits.defaults()));
    assertIiopCode(
        IiopDiagnosticCodes.INVALID_CONFIGURATION,
        () -> new IiopOptions(Duration.ZERO, Duration.ZERO, 1, 0, GiopLimits.defaults()));
    assertIiopCode(
        IiopDiagnosticCodes.INVALID_CONFIGURATION,
        () ->
            new IiopOptions(
                Duration.ofMillis((long) Integer.MAX_VALUE + 1),
                Duration.ZERO,
                1,
                1,
                GiopLimits.defaults()));

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          throw new IiopException(IiopDiagnosticCodes.LIFECYCLE, " ");
        });
    assertThrows(
        NullPointerException.class,
        () -> {
          throw new IiopException(IiopDiagnosticCodes.LIFECYCLE, null);
        });
  }

  @Test
  void serverRejectsNonRequestMessagesOnRequestPath() throws Exception {
    try (IiopServer server =
        IiopServer.bind(
            IiopEndpoint.loopback(0),
            IiopOptions.defaults(),
            request -> reply(request.requestId(), "unused"))) {
      try (Socket socket = new Socket(server.endpoint().host(), server.endpoint().port())) {
        socket.setSoTimeout(1_000);
        byte[] closeConnection =
            new GiopMessageWriter()
                .write(
                    new GiopCloseConnection(GiopHeader.forType(GiopMessageType.CLOSE_CONNECTION)));
        socket.getOutputStream().write(closeConnection);
        socket.getOutputStream().flush();

        assertEquals(-1, socket.getInputStream().read());
      }
    }
  }

  @Test
  void maxOpenConnectionBackpressureClosesExtraConnection() throws Exception {
    IiopOptions options =
        new IiopOptions(Duration.ofSeconds(1), Duration.ofSeconds(1), 2, 1, GiopLimits.defaults());
    CountDownLatch handlerEntered = new CountDownLatch(1);
    CountDownLatch releaseHandler = new CountDownLatch(1);
    AtomicReference<GiopReply> firstReply = new AtomicReference<>();
    AtomicReference<Throwable> firstFailure = new AtomicReference<>();

    try (IiopServer server =
            IiopServer.bind(
                IiopEndpoint.loopback(0),
                options,
                request -> {
                  handlerEntered.countDown();
                  if (!releaseHandler.await(2, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("test handler was not released");
                  }
                  return reply(request.requestId(), "first");
                });
        IiopClient firstClient = IiopClient.connect(server.endpoint(), options)) {
      Thread firstInvoke =
          new Thread(
              () -> {
                try {
                  firstReply.set(firstClient.invoke(helloRequest(3, "Dee")));
                } catch (Throwable throwable) {
                  firstFailure.set(throwable);
                }
              });
      firstInvoke.start();
      assertTrue(handlerEntered.await(1, TimeUnit.SECONDS));

      try (IiopClient secondClient = IiopClient.connect(server.endpoint(), options)) {
        assertIiopCode(IiopDiagnosticCodes.EOF, () -> secondClient.invoke(helloRequest(4, "Eve")));
      } finally {
        releaseHandler.countDown();
      }

      firstInvoke.join(2_000);
      assertNull(firstFailure.get());
      assertEquals("first", CdrReader.bigEndian(firstReply.get().body()).readString());
    }
  }

  @Test
  void closeOperationsAreIdempotentAndBlockFutureUse() {
    IiopServer server =
        IiopServer.bind(
            IiopEndpoint.loopback(0),
            IiopOptions.defaults(),
            request -> reply(request.requestId(), "ok"));
    IiopEndpoint endpoint = server.endpoint();
    server.close();
    server.close();

    assertIiopCode(
        IiopDiagnosticCodes.CONNECTION_FAILURE,
        () ->
            IiopClient.connect(
                endpoint,
                new IiopOptions(
                    Duration.ofMillis(100), Duration.ofMillis(100), 2, 2, GiopLimits.defaults())));

    try (IiopServer running =
        IiopServer.bind(
            IiopEndpoint.loopback(0),
            IiopOptions.defaults(),
            request -> reply(request.requestId(), "ok"))) {
      IiopClient client = IiopClient.connect(running.endpoint(), IiopOptions.defaults());
      client.close();
      client.close();
      assertIiopCode(IiopDiagnosticCodes.LIFECYCLE, () -> client.invoke(helloRequest(9, "Fox")));
    }
  }

  @Test
  void productionIiopSourcesStayWithinTcpProtocolBoundary() throws IOException {
    Path iiopSources = Path.of("src/main/java/io/github/mundanej/mjo/iiop");
    try (Stream<Path> paths =
        Files.find(
            iiopSources, Integer.MAX_VALUE, (path, attrs) -> path.toString().endsWith(".java"))) {
      List<String> joinedSources = paths.map(IiopTcpTest::readSource).toList();
      String source = String.join("\n", joinedSources);

      assertTrue(!source.contains("SSLContext.setDefault"));
      assertTrue(!source.contains("javax.net.ssl.keyStore"));
      assertTrue(!source.contains("javax.net.ssl.trustStore"));
      assertTrue(!source.contains("HttpsURLConnection"));
      assertTrue(!source.contains("io.github.mundanej.mjo.orb"));
      assertTrue(!source.contains("io.github.mundanej.mjo.poa"));
      assertTrue(!source.contains("java.lang.reflect"));
      assertTrue(!source.contains("ObjectInputStream"));
      assertTrue(!source.contains("ObjectOutputStream"));
    }
  }

  private GiopReply handleHello(GiopRequest request) {
    assertEquals("greet", request.operation());
    assertArrayEquals(
        "hello".getBytes(java.nio.charset.StandardCharsets.US_ASCII), request.objectKey());
    String name = CdrReader.bigEndian(request.body()).readString();
    return reply(request.requestId(), "Hello, " + name);
  }

  private static GiopRequest helloRequest(long requestId, String name) {
    return new GiopRequest(
        GiopHeader.forType(GiopMessageType.REQUEST),
        requestId,
        3,
        "hello".getBytes(java.nio.charset.StandardCharsets.US_ASCII),
        "greet",
        List.of(),
        CdrWriter.bigEndian().writeString(name).toByteArray());
  }

  private static GiopReply reply(long requestId, String value) {
    return new GiopReply(
        GiopHeader.forType(GiopMessageType.REPLY),
        requestId,
        GiopReplyStatus.NO_EXCEPTION,
        List.of(),
        CdrWriter.bigEndian().writeString(value).toByteArray());
  }

  private static void assertIiopCode(Object expectedCode, ThrowingRunnable runnable) {
    IiopException exception = assertThrows(IiopException.class, runnable::run);
    assertEquals(expectedCode, exception.code());
  }

  private static String readSource(Path path) {
    try {
      return Files.readString(path);
    } catch (IOException exception) {
      throw new IllegalStateException("Could not read " + path, exception);
    }
  }

  private static byte[] bytes(int... values) {
    byte[] bytes = new byte[values.length];
    for (int index = 0; index < values.length; index++) {
      bytes[index] = (byte) values[index];
    }
    return bytes;
  }

  @FunctionalInterface
  private interface ThrowingRunnable {

    void run() throws Exception;
  }
}
