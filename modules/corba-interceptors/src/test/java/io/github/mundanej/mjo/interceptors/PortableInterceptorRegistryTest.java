package io.github.mundanej.mjo.interceptors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjo.common.DiagnosticCode;
import io.github.mundanej.mjo.giop.GiopReplyStatus;
import io.github.mundanej.mjo.giop.GiopServiceContext;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit tests for local Portable Interceptor registration and request contexts. */
@Tag("unit")
final class PortableInterceptorRegistryTest {

  @Test
  void emptyRegistryIsReusableAndHasNoSideEffects() {
    PortableInterceptorRegistry registry = PortableInterceptorRegistry.empty();
    ClientRequestContext clientContext = new ClientRequestContext(0, "op", List.of());
    ServerRequestContext serverContext =
        new ServerRequestContext(0, "op", new byte[] {1, 2}, List.of());

    registry.sendClientRequest(clientContext);
    registry.receiveClientReply(clientContext);
    registry.receiveClientException(clientContext);
    registry.receiveServerRequestServiceContexts(serverContext);
    registry.receiveServerRequest(serverContext);
    registry.sendServerReply(serverContext);
    registry.sendServerException(serverContext);

    assertTrue(clientContext.requestServiceContexts().isEmpty());
    assertTrue(clientContext.replyServiceContexts().isEmpty());
    assertTrue(clientContext.replyStatus().isEmpty());
    assertTrue(serverContext.replyServiceContexts().isEmpty());
    assertTrue(serverContext.replyStatus().isEmpty());
  }

  @Test
  void invokesClientAndServerInterceptorsInDeterministicOrder() {
    List<String> calls = new ArrayList<>();
    PortableInterceptorRegistry registry =
        PortableInterceptorRegistry.builder()
            .addClient(client("client-a", calls))
            .addClient(client("client-b", calls))
            .addServer(server("server-a", calls))
            .addServer(server("server-b", calls))
            .build();
    ClientRequestContext clientContext = new ClientRequestContext(1, "op", List.of());
    ServerRequestContext serverContext =
        new ServerRequestContext(1, "op", new byte[] {1}, List.of());

    registry.sendClientRequest(clientContext);
    clientContext.completeReply(GiopReplyStatus.NO_EXCEPTION, List.of());
    registry.receiveClientReply(clientContext);
    registry.receiveServerRequestServiceContexts(serverContext);
    registry.receiveServerRequest(serverContext);
    registry.sendServerReply(serverContext);

    assertEquals(
        List.of(
            "client-a:send",
            "client-b:send",
            "client-b:reply",
            "client-a:reply",
            "server-a:contexts",
            "server-b:contexts",
            "server-a:request",
            "server-b:request",
            "server-b:reply",
            "server-a:reply"),
        calls);
  }

  @Test
  void invokesExceptionCallbacksInReverseOrder() {
    List<String> calls = new ArrayList<>();
    PortableInterceptorRegistry registry =
        PortableInterceptorRegistry.builder()
            .addClient(exceptionClient("client-a", calls))
            .addClient(exceptionClient("client-b", calls))
            .addServer(exceptionServer("server-a", calls))
            .addServer(exceptionServer("server-b", calls))
            .build();
    ClientRequestContext clientContext = new ClientRequestContext(2, "op", List.of());
    ServerRequestContext serverContext =
        new ServerRequestContext(2, "op", new byte[] {3}, List.of());

    registry.receiveClientException(clientContext);
    registry.sendServerException(serverContext);

    assertEquals(
        List.of(
            "client-b:exception", "client-a:exception", "server-b:exception", "server-a:exception"),
        calls);
  }

  @Test
  void validatesDuplicateNamesAndServiceContextReplacement() {
    InterceptorException duplicateName =
        assertThrows(
            InterceptorException.class,
            () ->
                PortableInterceptorRegistry.builder()
                    .addClient(client("same", new ArrayList<>()))
                    .addClient(client("same", new ArrayList<>()))
                    .build());
    ClientRequestContext context = new ClientRequestContext(1, "op", List.of());
    context.addRequestServiceContext(new GiopServiceContext(7, new byte[] {1}), false);

    InterceptorException duplicateContext =
        assertThrows(
            InterceptorException.class,
            () ->
                context.addRequestServiceContext(new GiopServiceContext(7, new byte[] {2}), false));
    context.addRequestServiceContext(new GiopServiceContext(7, new byte[] {3}), true);

    assertEquals(InterceptorDiagnosticCodes.DUPLICATE_INTERCEPTOR, duplicateName.code());
    assertEquals(InterceptorDiagnosticCodes.DUPLICATE_SERVICE_CONTEXT, duplicateContext.code());
    assertEquals(
        List.of(new GiopServiceContext(7, new byte[] {3})), context.requestServiceContexts());
  }

  @Test
  void validatesServerNamesAndReplyServiceContexts() {
    InterceptorException duplicateName =
        assertThrows(
            InterceptorException.class,
            () ->
                PortableInterceptorRegistry.builder()
                    .addServer(server("same", new ArrayList<>()))
                    .addServer(server("same", new ArrayList<>()))
                    .build());
    ServerRequestContext context =
        new ServerRequestContext(
            1, "op", new byte[] {9}, List.of(new GiopServiceContext(1, new byte[] {2})));
    context.addReplyServiceContext(new GiopServiceContext(8, new byte[] {1}), false);

    InterceptorException duplicateContext =
        assertThrows(
            InterceptorException.class,
            () -> context.addReplyServiceContext(new GiopServiceContext(8, new byte[] {2}), false));
    context.addReplyServiceContext(new GiopServiceContext(8, new byte[] {3}), true);
    context.replyStatus(GiopReplyStatus.USER_EXCEPTION);

    assertEquals(InterceptorDiagnosticCodes.DUPLICATE_INTERCEPTOR, duplicateName.code());
    assertEquals(InterceptorDiagnosticCodes.DUPLICATE_SERVICE_CONTEXT, duplicateContext.code());
    assertEquals(
        List.of(new GiopServiceContext(8, new byte[] {3})), context.replyServiceContexts());
    assertEquals(GiopReplyStatus.USER_EXCEPTION, context.replyStatus().orElseThrow());
    assertEquals(
        List.of(new GiopServiceContext(1, new byte[] {2})), context.requestServiceContexts());
  }

  @Test
  void wrapsRuntimeCallbackFailuresDeterministically() {
    PortableInterceptorRegistry registry =
        PortableInterceptorRegistry.builder()
            .addClient(
                new PortableClientRequestInterceptor() {
                  @Override
                  public String name() {
                    return "broken";
                  }

                  @Override
                  public void sendRequest(ClientRequestContext context) {
                    throw new IllegalStateException("boom");
                  }
                })
            .build();

    InterceptorException exception =
        assertThrows(
            InterceptorException.class,
            () -> registry.sendClientRequest(new ClientRequestContext(1, "op", List.of())));

    assertEquals(InterceptorDiagnosticCodes.CALLBACK_FAILURE, exception.code());
    assertInstanceOf(IllegalStateException.class, exception.getCause());
  }

  @Test
  void preservesExplicitInterceptorExceptions() {
    InterceptorException original =
        new InterceptorException(
            InterceptorDiagnosticCodes.DUPLICATE_INTERCEPTOR, "already mapped");
    PortableInterceptorRegistry registry =
        PortableInterceptorRegistry.builder()
            .addClient(
                new PortableClientRequestInterceptor() {
                  @Override
                  public String name() {
                    return "explicit";
                  }

                  @Override
                  public void sendRequest(ClientRequestContext context) {
                    throw original;
                  }
                })
            .build();

    InterceptorException thrown =
        assertThrows(
            InterceptorException.class,
            () -> registry.sendClientRequest(new ClientRequestContext(1, "op", List.of())));

    assertEquals(original, thrown);
    assertEquals(InterceptorDiagnosticCodes.DUPLICATE_INTERCEPTOR, thrown.code());
  }

  @Test
  void validatesContextInputsAndDefensiveCopies() {
    byte[] objectKey = {1, 2};
    List<GiopServiceContext> serviceContexts = new ArrayList<>();
    serviceContexts.add(new GiopServiceContext(5, new byte[] {6}));

    ClientRequestContext clientContext = new ClientRequestContext(3, "op", serviceContexts);
    ServerRequestContext serverContext =
        new ServerRequestContext(4, "op", objectKey, serviceContexts);
    serviceContexts.clear();
    objectKey[0] = 9;
    byte[] returnedKey = serverContext.objectKey();
    returnedKey[1] = 9;

    assertThrows(
        IllegalArgumentException.class, () -> new ClientRequestContext(-1, "op", List.of()));
    assertThrows(
        IllegalArgumentException.class,
        () -> new ClientRequestContext(0x1_0000_0000L, "op", List.of()));
    assertThrows(IllegalArgumentException.class, () -> new ClientRequestContext(1, " ", List.of()));
    assertThrows(NullPointerException.class, () -> clientContext.completeReply(null, List.of()));
    assertThrows(
        UnsupportedOperationException.class,
        () ->
            clientContext.requestServiceContexts().add(new GiopServiceContext(9, new byte[] {1})));
    assertThrows(
        UnsupportedOperationException.class,
        () ->
            serverContext.requestServiceContexts().add(new GiopServiceContext(9, new byte[] {1})));
    assertThrows(
        IllegalArgumentException.class,
        () -> new ServerRequestContext(-1, "op", objectKey, List.of()));
    assertThrows(
        IllegalArgumentException.class,
        () -> new ServerRequestContext(0x1_0000_0000L, "op", objectKey, List.of()));
    assertThrows(
        IllegalArgumentException.class,
        () -> new ServerRequestContext(1, "", objectKey, List.of()));
    assertThrows(
        NullPointerException.class, () -> PortableInterceptorRegistry.builder().addClient(null));
    assertThrows(
        NullPointerException.class, () -> PortableInterceptorRegistry.builder().addServer(null));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            PortableInterceptorRegistry.builder()
                .addClient(client(" ", new ArrayList<>()))
                .build());
    assertEquals(
        List.of(new GiopServiceContext(5, new byte[] {6})), clientContext.requestServiceContexts());
    assertArrayEquals(new byte[] {1, 2}, serverContext.objectKey());
    assertEquals(
        List.of(new GiopServiceContext(5, new byte[] {6})), serverContext.requestServiceContexts());
  }

  @Test
  void validatesInterceptorExceptionMessagesAndCauses() {
    IllegalStateException cause = new IllegalStateException("boom");
    InterceptorException exception =
        new InterceptorException(new DiagnosticCode("PI-9999"), "message", cause);

    assertEquals(new DiagnosticCode("PI-9999"), exception.code());
    assertEquals("message", exception.getMessage());
    assertEquals(cause, exception.getCause());
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          throw new InterceptorException(InterceptorDiagnosticCodes.CALLBACK_FAILURE, " ");
        });
  }

  private static PortableClientRequestInterceptor client(String name, List<String> calls) {
    return new PortableClientRequestInterceptor() {
      @Override
      public String name() {
        return name;
      }

      @Override
      public void sendRequest(ClientRequestContext context) {
        calls.add(name + ":send");
      }

      @Override
      public void receiveReply(ClientRequestContext context) {
        calls.add(name + ":reply");
      }
    };
  }

  private static PortableClientRequestInterceptor exceptionClient(String name, List<String> calls) {
    return new PortableClientRequestInterceptor() {
      @Override
      public String name() {
        return name;
      }

      @Override
      public void receiveException(ClientRequestContext context) {
        calls.add(name + ":exception");
      }
    };
  }

  private static PortableServerRequestInterceptor server(String name, List<String> calls) {
    return new PortableServerRequestInterceptor() {
      @Override
      public String name() {
        return name;
      }

      @Override
      public void receiveRequestServiceContexts(ServerRequestContext context) {
        calls.add(name + ":contexts");
      }

      @Override
      public void receiveRequest(ServerRequestContext context) {
        calls.add(name + ":request");
      }

      @Override
      public void sendReply(ServerRequestContext context) {
        calls.add(name + ":reply");
      }
    };
  }

  private static PortableServerRequestInterceptor exceptionServer(String name, List<String> calls) {
    return new PortableServerRequestInterceptor() {
      @Override
      public String name() {
        return name;
      }

      @Override
      public void sendException(ServerRequestContext context) {
        calls.add(name + ":exception");
      }
    };
  }
}
