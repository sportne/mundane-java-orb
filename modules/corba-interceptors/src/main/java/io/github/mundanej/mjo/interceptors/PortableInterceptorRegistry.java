package io.github.mundanej.mjo.interceptors;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Immutable ordered registry of local Portable Interceptor callbacks. */
public final class PortableInterceptorRegistry {

  private static final PortableInterceptorRegistry EMPTY =
      new PortableInterceptorRegistry(List.of(), List.of());

  private final List<PortableClientRequestInterceptor> clientInterceptors;
  private final List<PortableServerRequestInterceptor> serverInterceptors;

  private PortableInterceptorRegistry(
      List<PortableClientRequestInterceptor> clientInterceptors,
      List<PortableServerRequestInterceptor> serverInterceptors) {
    this.clientInterceptors = List.copyOf(clientInterceptors);
    this.serverInterceptors = List.copyOf(serverInterceptors);
  }

  /** Returns an empty interceptor registry. */
  public static PortableInterceptorRegistry empty() {
    return EMPTY;
  }

  /** Creates a registry builder. */
  public static Builder builder() {
    return new Builder();
  }

  /** Invokes client send-request callbacks in registration order. */
  public void sendClientRequest(ClientRequestContext context) {
    for (PortableClientRequestInterceptor interceptor : clientInterceptors) {
      invoke(interceptor.name(), () -> interceptor.sendRequest(context));
    }
  }

  /** Invokes client receive-reply callbacks in reverse registration order. */
  public void receiveClientReply(ClientRequestContext context) {
    for (int index = clientInterceptors.size() - 1; index >= 0; index--) {
      PortableClientRequestInterceptor interceptor = clientInterceptors.get(index);
      invoke(interceptor.name(), () -> interceptor.receiveReply(context));
    }
  }

  /** Invokes client receive-exception callbacks in reverse registration order. */
  public void receiveClientException(ClientRequestContext context) {
    for (int index = clientInterceptors.size() - 1; index >= 0; index--) {
      PortableClientRequestInterceptor interceptor = clientInterceptors.get(index);
      invoke(interceptor.name(), () -> interceptor.receiveException(context));
    }
  }

  /** Invokes server service-context callbacks in registration order. */
  public void receiveServerRequestServiceContexts(ServerRequestContext context) {
    for (PortableServerRequestInterceptor interceptor : serverInterceptors) {
      invoke(interceptor.name(), () -> interceptor.receiveRequestServiceContexts(context));
    }
  }

  /** Invokes server receive-request callbacks in registration order. */
  public void receiveServerRequest(ServerRequestContext context) {
    for (PortableServerRequestInterceptor interceptor : serverInterceptors) {
      invoke(interceptor.name(), () -> interceptor.receiveRequest(context));
    }
  }

  /** Invokes server send-reply callbacks in reverse registration order. */
  public void sendServerReply(ServerRequestContext context) {
    for (int index = serverInterceptors.size() - 1; index >= 0; index--) {
      PortableServerRequestInterceptor interceptor = serverInterceptors.get(index);
      invoke(interceptor.name(), () -> interceptor.sendReply(context));
    }
  }

  /** Invokes server send-exception callbacks in reverse registration order. */
  public void sendServerException(ServerRequestContext context) {
    for (int index = serverInterceptors.size() - 1; index >= 0; index--) {
      PortableServerRequestInterceptor interceptor = serverInterceptors.get(index);
      invoke(interceptor.name(), () -> interceptor.sendException(context));
    }
  }

  private static void invoke(String name, Callback callback) {
    try {
      callback.invoke();
    } catch (InterceptorException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      throw new InterceptorException(
          InterceptorDiagnosticCodes.CALLBACK_FAILURE,
          "interceptor callback failed: " + name,
          exception);
    }
  }

  /** Builder for immutable interceptor registries. */
  public static final class Builder {

    private final java.util.ArrayList<PortableClientRequestInterceptor> clientInterceptors =
        new java.util.ArrayList<>();
    private final java.util.ArrayList<PortableServerRequestInterceptor> serverInterceptors =
        new java.util.ArrayList<>();

    private Builder() {}

    /** Adds one client request interceptor. */
    public Builder addClient(PortableClientRequestInterceptor interceptor) {
      clientInterceptors.add(Objects.requireNonNull(interceptor, "interceptor"));
      return this;
    }

    /** Adds one server request interceptor. */
    public Builder addServer(PortableServerRequestInterceptor interceptor) {
      serverInterceptors.add(Objects.requireNonNull(interceptor, "interceptor"));
      return this;
    }

    /** Builds an immutable registry after validating unique names per side. */
    public PortableInterceptorRegistry build() {
      validateUniqueNames(
          clientInterceptors.stream().map(PortableClientRequestInterceptor::name).toList());
      validateUniqueNames(
          serverInterceptors.stream().map(PortableServerRequestInterceptor::name).toList());
      return new PortableInterceptorRegistry(clientInterceptors, serverInterceptors);
    }

    private static void validateUniqueNames(List<String> names) {
      Set<String> seen = new LinkedHashSet<>();
      for (String name : names) {
        String checked = requireNonBlank(name, "interceptor name");
        if (!seen.add(checked)) {
          throw new InterceptorException(
              InterceptorDiagnosticCodes.DUPLICATE_INTERCEPTOR,
              "duplicate interceptor name: " + checked);
        }
      }
    }
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }

  @FunctionalInterface
  private interface Callback {
    void invoke();
  }
}
