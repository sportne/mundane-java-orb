package io.github.mundanej.mjo.naming.server;

import io.github.mundanej.mjo.cdr.CdrByteOrder;
import io.github.mundanej.mjo.common.DiagnosticCode;
import io.github.mundanej.mjo.giop.GiopHeader;
import io.github.mundanej.mjo.giop.GiopMessageType;
import io.github.mundanej.mjo.giop.GiopReply;
import io.github.mundanej.mjo.giop.GiopReplyStatus;
import io.github.mundanej.mjo.giop.GiopRequest;
import io.github.mundanej.mjo.giop.GiopSystemExceptionBody;
import io.github.mundanej.mjo.iiop.IiopClient;
import io.github.mundanej.mjo.iiop.IiopEndpoint;
import io.github.mundanej.mjo.iiop.IiopObjectReference;
import io.github.mundanej.mjo.iiop.IiopOptions;
import io.github.mundanej.mjo.ior.CorbalocAddress;
import io.github.mundanej.mjo.ior.CorbalocUrl;
import io.github.mundanej.mjo.ior.CorbanameUrl;
import io.github.mundanej.mjo.ior.IiopProfile;
import io.github.mundanej.mjo.ior.IiopVersion;
import io.github.mundanej.mjo.ior.Ior;
import io.github.mundanej.mjo.ior.ObjectKey;
import io.github.mundanej.mjo.ior.TaggedProfile;
import io.github.mundanej.mjo.naming.NamingDiagnosticCodes;
import io.github.mundanej.mjo.naming.NamingException;
import io.github.mundanej.mjo.naming.NamingName;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/** Client for the bounded G10 network Naming Service lane. */
public final class NetworkNamingClient implements AutoCloseable {

  private final IiopObjectReference reference;
  private final IiopClient client;
  private final AtomicLong nextRequestId = new AtomicLong(1);

  private NetworkNamingClient(IiopObjectReference reference, IiopClient client) {
    this.reference = Objects.requireNonNull(reference, "reference");
    this.client = Objects.requireNonNull(client, "client");
  }

  /** Connects to a NamingContext IOR. */
  public static NetworkNamingClient connect(Ior ior, IiopOptions options) {
    IiopObjectReference reference = IiopObjectReference.fromIor(ior);
    return new NetworkNamingClient(reference, IiopClient.connect(reference.endpoint(), options));
  }

  /** Connects to a corbaloc URL. */
  public static NetworkNamingClient connect(CorbalocUrl url, IiopOptions options) {
    return connect(ior(url), options);
  }

  /** Resolves a corbaname URL by opening a bounded Naming Service connection. */
  public static RemoteNamingBindingTarget resolve(CorbanameUrl url, IiopOptions options) {
    try (NetworkNamingClient client = connect(url.location(), options)) {
      if (url.stringName().isEmpty()) {
        return RemoteNamingBindingTarget.context(client.reference.ior());
      }
      return client.resolve(NamingName.parse(url.stringName()));
    }
  }

  /** Binds an object IOR. */
  public void bind(NamingName name, Ior ior) {
    invokeEmpty("bind", NetworkNamingService.writeBindingRequest(name, ior));
  }

  /** Binds or replaces an object IOR. */
  public void rebind(NamingName name, Ior ior) {
    invokeEmpty("rebind", NetworkNamingService.writeBindingRequest(name, ior));
  }

  /** Resolves a name to an object or context IOR. */
  public RemoteNamingBindingTarget resolve(NamingName name) {
    return NetworkNamingService.readTarget(
        invoke("resolve", NetworkNamingService.writeName(name)),
        RemoteNamingBindingTarget.Kind.OBJECT);
  }

  /** Removes a binding. */
  public void unbind(NamingName name) {
    invokeEmpty("unbind", NetworkNamingService.writeName(name));
  }

  /** Creates and binds a child naming context. */
  public RemoteNamingBindingTarget bindNewContext(NamingName name) {
    return NetworkNamingService.readTarget(
        invoke("bind_new_context", NetworkNamingService.writeName(name)),
        RemoteNamingBindingTarget.Kind.CONTEXT);
  }

  /** Lists immediate bindings. */
  public List<RemoteNamingBinding> list(int count) {
    return NetworkNamingService.readBindings(
        invoke("list", NetworkNamingService.writeCount(count)));
  }

  /** Destroys this naming context. */
  public void destroy() {
    invokeEmpty("destroy", new byte[0]);
  }

  @Override
  public void close() {
    client.close();
  }

  private void invokeEmpty(String operation, byte[] body) {
    byte[] reply = invoke(operation, body);
    if (reply.length != 0) {
      throw new NamingException(
          NamingDiagnosticCodes.INVALID_NAME, "Naming Service operation returned trailing payload");
    }
  }

  private byte[] invoke(String operation, byte[] body) {
    GiopReply reply =
        client.invoke(
            new GiopRequest(
                GiopHeader.forType(GiopMessageType.REQUEST),
                nextRequestId.getAndIncrement(),
                3,
                reference.objectKey(),
                operation,
                List.of(),
                body));
    if (reply.replyStatus() == GiopReplyStatus.NO_EXCEPTION) {
      return reply.body();
    }
    if (reply.replyStatus() == GiopReplyStatus.SYSTEM_EXCEPTION) {
      throw namingException(reply);
    }
    throw new NamingException(
        NamingDiagnosticCodes.UNSUPPORTED_LOCATION,
        "Unsupported Naming Service reply status: " + reply.replyStatus());
  }

  private static RuntimeException namingException(GiopReply reply) {
    GiopSystemExceptionBody body =
        GiopSystemExceptionBody.fromBytes(CdrByteOrder.BIG_ENDIAN, reply.body());
    DiagnosticCode code = codeFromMinor(body.minorCodeValue());
    if (code != null) {
      return new NamingException(code, "remote Naming Service rejected the request");
    }
    if (body.repositoryId().endsWith("/OBJECT_NOT_EXIST:1.0")) {
      return new NamingException(
          NamingDiagnosticCodes.NOT_FOUND, "remote Naming Service binding was not found");
    }
    if (body.repositoryId().endsWith("/BAD_OPERATION:1.0")) {
      return new NamingException(
          NamingDiagnosticCodes.UNSUPPORTED_LOCATION,
          "remote Naming Service operation unsupported");
    }
    return new NamingException(
        NamingDiagnosticCodes.INVALID_NAME, "remote Naming Service rejected the request");
  }

  private static Ior ior(CorbalocUrl url) {
    Objects.requireNonNull(url, "url");
    CorbalocAddress address =
        url.addresses().stream()
            .filter(candidate -> candidate.kind() == CorbalocAddress.Kind.IIOP)
            .findFirst()
            .orElseThrow(
                () ->
                    new NamingException(
                        NamingDiagnosticCodes.UNSUPPORTED_LOCATION,
                        "corbaloc URL does not contain an IIOP address"));
    byte[] objectKey =
        url.keyString().isEmpty()
            ? NetworkNamingService.NAME_SERVICE_KEY.getBytes(StandardCharsets.US_ASCII)
            : url.objectKey().octets();
    IiopProfile profile =
        new IiopProfile(
            address.version().orElse(IiopVersion.V1_2),
            address.host().orElse(""),
            address.port().orElseThrow(),
            new ObjectKey(objectKey),
            List.of());
    return new Ior(
        "IDL:omg.org/CosNaming/NamingContextExt:1.0", List.of(TaggedProfile.internetIop(profile)));
  }

  /** Returns the endpoint used by this client. */
  public IiopEndpoint endpoint() {
    return client.endpoint();
  }

  private static DiagnosticCode codeFromMinor(long minorCodeValue) {
    int minor = (int) minorCodeValue;
    return switch (minor) {
      case 1 -> NamingDiagnosticCodes.INVALID_NAME;
      case 2 -> NamingDiagnosticCodes.NOT_FOUND;
      case 3 -> NamingDiagnosticCodes.ALREADY_BOUND;
      case 4 -> NamingDiagnosticCodes.NOT_CONTEXT;
      case 5 -> NamingDiagnosticCodes.NOT_EMPTY;
      case 6 -> NamingDiagnosticCodes.DESTROYED;
      case 7 -> NamingDiagnosticCodes.ITERATOR_CLOSED;
      case 8 -> NamingDiagnosticCodes.UNSUPPORTED_LOCATION;
      default -> null;
    };
  }
}
