package io.github.mundanej.mjo.naming.server;

import io.github.mundanej.mjo.cdr.CdrByteOrder;
import io.github.mundanej.mjo.cdr.CdrReader;
import io.github.mundanej.mjo.cdr.CdrWriter;
import io.github.mundanej.mjo.common.DiagnosticCode;
import io.github.mundanej.mjo.giop.GiopCompletionStatus;
import io.github.mundanej.mjo.giop.GiopHeader;
import io.github.mundanej.mjo.giop.GiopMessageType;
import io.github.mundanej.mjo.giop.GiopReply;
import io.github.mundanej.mjo.giop.GiopReplyStatus;
import io.github.mundanej.mjo.giop.GiopRequest;
import io.github.mundanej.mjo.giop.GiopSystemExceptionBody;
import io.github.mundanej.mjo.giop.GiopTargetAddress;
import io.github.mundanej.mjo.iiop.IiopEndpoint;
import io.github.mundanej.mjo.iiop.IiopObjectReference;
import io.github.mundanej.mjo.iiop.IiopOptions;
import io.github.mundanej.mjo.iiop.IiopServer;
import io.github.mundanej.mjo.ior.IiopProfile;
import io.github.mundanej.mjo.ior.IiopVersion;
import io.github.mundanej.mjo.ior.Ior;
import io.github.mundanej.mjo.ior.ObjectKey;
import io.github.mundanej.mjo.ior.TaggedProfile;
import io.github.mundanej.mjo.naming.NameComponent;
import io.github.mundanej.mjo.naming.NamingDiagnosticCodes;
import io.github.mundanej.mjo.naming.NamingException;
import io.github.mundanej.mjo.naming.NamingName;
import io.github.mundanej.mjo.orb.DurableObjectKey;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.SystemException;

/** Bounded IIOP-exposed Naming Service fixture for G10 interoperability lanes. */
public final class NetworkNamingService implements AutoCloseable {

  /** Standard network object key for the root Naming Service context. */
  public static final String NAME_SERVICE_KEY = "NameService";

  private static final String NAMING_CONTEXT_REPOSITORY_ID =
      "IDL:omg.org/CosNaming/NamingContextExt:1.0";

  private final IiopServer server;
  private final Handler handler;

  private NetworkNamingService(IiopServer server, Handler handler) {
    this.server = Objects.requireNonNull(server, "server");
    this.handler = Objects.requireNonNull(handler, "handler");
  }

  /** Starts a network Naming Service on the supplied endpoint. */
  public static NetworkNamingService bind(IiopEndpoint endpoint, IiopOptions options) {
    Handler handler = new Handler();
    IiopServer server = IiopServer.bind(endpoint, options, handler);
    try {
      handler.endpoint(server.endpoint());
    } catch (RuntimeException exception) {
      server.close();
      throw exception;
    }
    return new NetworkNamingService(server, handler);
  }

  /** Starts a durable network Naming Service with caller-configured persistence. */
  public static NetworkNamingService bind(
      IiopEndpoint endpoint, IiopOptions options, NamingPersistenceOptions persistenceOptions) {
    Handler handler = new Handler(persistenceOptions);
    IiopServer server = IiopServer.bind(endpoint, options, handler);
    try {
      handler.endpoint(server.endpoint());
    } catch (RuntimeException exception) {
      server.close();
      throw exception;
    }
    return new NetworkNamingService(server, handler);
  }

  /** Returns the actual bound endpoint. */
  public IiopEndpoint endpoint() {
    return server.endpoint();
  }

  /** Returns the root NamingContext IOR. */
  public Ior ior() {
    return handler.ior(handler.rootObjectKey());
  }

  /** Returns a corbaloc URL for the root NamingContext. */
  public String corbaloc() {
    return "corbaloc:iiop:1.2@"
        + endpoint().host()
        + ":"
        + endpoint().port()
        + "/"
        + percentEncode(handler.rootObjectKey().octets());
  }

  @Override
  public void close() {
    server.close();
  }

  private static final class Handler implements io.github.mundanej.mjo.iiop.IiopRequestHandler {

    private final Map<ObjectKey, ContextState> contexts = new LinkedHashMap<>();
    private IiopEndpoint endpoint;
    private int nextContextId = 1;
    private final NamingPersistenceOptions persistenceOptions;
    private final NamingPersistenceStore persistenceStore;
    private ObjectKey rootKey;

    private Handler() {
      this.persistenceOptions = null;
      this.persistenceStore = null;
      this.rootKey = asciiObjectKey(NAME_SERVICE_KEY);
      contexts.put(rootKey, new ContextState());
    }

    private Handler(NamingPersistenceOptions persistenceOptions) {
      this.persistenceOptions = Objects.requireNonNull(persistenceOptions, "persistenceOptions");
      this.persistenceStore = new NamingPersistenceStore(persistenceOptions);
      this.rootKey = durableContextObjectKey(NAME_SERVICE_KEY);
      NamingPersistenceStore.StoredState stored = persistenceStore.load();
      this.nextContextId = stored.nextContextId();
      if (stored.contexts().isEmpty()) {
        contexts.put(rootKey, new ContextState());
      } else {
        for (NamingPersistenceStore.StoredContext storedContext : stored.contexts()) {
          ObjectKey key = objectKey(storedContext.ior());
          if (contexts.put(
                  key, new ContextState(storedContext.destroyed(), storedContext.bindings()))
              != null) {
            throw new NamingException(
                NamingDiagnosticCodes.INVALID_NAME,
                "Naming persistence store contains duplicate contexts");
          }
        }
        if (!contexts.containsKey(rootKey)) {
          throw new NamingException(
              NamingDiagnosticCodes.INVALID_NAME,
              "Naming persistence store does not contain the root context");
        }
        validateStoredContextTargets();
      }
    }

    private synchronized void endpoint(IiopEndpoint endpoint) {
      this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
      if (persistenceStore != null && !persistenceStore.exists()) {
        persist();
      }
    }

    @Override
    public synchronized GiopReply handle(GiopRequest request) {
      Objects.requireNonNull(request, "request");
      try {
        ContextState context = context(objectKey(request.targetAddress()));
        byte[] body = dispatch(context, request.operation(), request.body());
        return new GiopReply(
            replyHeader(request),
            request.requestId(),
            GiopReplyStatus.NO_EXCEPTION,
            request.serviceContexts(),
            body);
      } catch (SystemException exception) {
        return systemExceptionReply(request, exception);
      } catch (NamingException exception) {
        return systemExceptionReply(request, systemException(exception));
      } catch (RuntimeException exception) {
        return systemExceptionReply(
            request,
            new org.omg.CORBA.UNKNOWN(
                exception.getMessage(), exception, 0, CompletionStatus.COMPLETED_MAYBE));
      }
    }

    private byte[] dispatch(ContextState context, String operation, byte[] body) {
      return switch (operation) {
        case "bind" -> {
          BindingRequest request = BindingRequest.read(body);
          yield mutate(
              () -> {
                bind(
                    context,
                    request.name(),
                    RemoteNamingBindingTarget.object(request.ior()),
                    false);
                return empty();
              });
        }
        case "rebind" -> {
          BindingRequest request = BindingRequest.read(body);
          yield mutate(
              () -> {
                bind(
                    context, request.name(), RemoteNamingBindingTarget.object(request.ior()), true);
                return empty();
              });
        }
        case "resolve" -> writeTarget(resolve(context, readName(body)));
        case "unbind" -> {
          NamingName name = readName(body);
          yield mutate(
              () -> {
                unbind(context, name);
                return empty();
              });
        }
        case "bind_new_context" -> {
          NamingName name = readName(body);
          yield mutate(() -> writeTarget(bindNewContext(context, name)));
        }
        case "list" -> writeBindings(context.list(readCount(body)));
        case "destroy" -> {
          yield mutate(
              () -> {
                context.destroy();
                return empty();
              });
        }
        default ->
            throw new org.omg.CORBA.BAD_OPERATION(
                "Unknown Naming Service operation: " + operation, 0, CompletionStatus.COMPLETED_NO);
      };
    }

    private RemoteNamingBindingTarget bindNewContext(ContextState context, NamingName name) {
      String contextId = "context-" + nextContextId;
      ObjectKey key = contextObjectKey(contextId);
      ContextState child = new ContextState();
      RemoteNamingBindingTarget target = RemoteNamingBindingTarget.context(ior(key));
      bind(context, name, target, false);
      contexts.put(key, child);
      nextContextId++;
      return target;
    }

    private void bind(
        ContextState context, NamingName name, RemoteNamingBindingTarget target, boolean replace) {
      if (persistenceStore != null) {
        persistenceStore.requireDurableIor(target.ior(), "Naming binding target");
      }
      ResolvedParent parent = parent(context, name);
      parent.context().bind(parent.leaf(), target, replace);
    }

    private RemoteNamingBindingTarget resolve(ContextState context, NamingName name) {
      ContextState current = context;
      List<NameComponent> components = name.components();
      for (int index = 0; index < components.size(); index++) {
        RemoteNamingBindingTarget target = current.resolve(components.get(index));
        if (index == components.size() - 1) {
          return target;
        }
        current = requireContext(target);
      }
      throw new NamingException(NamingDiagnosticCodes.INVALID_NAME, "name must not be empty");
    }

    private void unbind(ContextState context, NamingName name) {
      ResolvedParent parent = parent(context, name);
      parent.context().unbind(parent.leaf());
    }

    private ResolvedParent parent(ContextState context, NamingName name) {
      ContextState current = context;
      for (NameComponent component : name.parentComponents()) {
        current = requireContext(current.resolve(component));
      }
      return new ResolvedParent(current, name.leaf());
    }

    private ContextState requireContext(RemoteNamingBindingTarget target) {
      if (target.kind() != RemoteNamingBindingTarget.Kind.CONTEXT) {
        throw new NamingException(
            NamingDiagnosticCodes.NOT_CONTEXT, "name component is not a context");
      }
      ContextState context = contexts.get(objectKey(target.ior()));
      if (context == null || context.destroyed()) {
        throw new NamingException(
            NamingDiagnosticCodes.NOT_FOUND, "naming context target is not available");
      }
      return context;
    }

    private ContextState context(byte[] objectKey) {
      ContextState context = contexts.get(new ObjectKey(objectKey));
      if (context == null || context.destroyed()) {
        throw new org.omg.CORBA.OBJECT_NOT_EXIST(
            "Unknown Naming Service object key", 0, CompletionStatus.COMPLETED_NO);
      }
      return context;
    }

    private static byte[] objectKey(GiopTargetAddress targetAddress) {
      return switch (targetAddress.discriminator()) {
        case GiopTargetAddress.KEY_ADDR -> targetAddress.objectKey();
        case GiopTargetAddress.PROFILE_ADDR -> objectKey(targetAddress.profile());
        case GiopTargetAddress.REFERENCE_ADDR -> objectKeyFromReferenceAddr(targetAddress);
        default ->
            throw new org.omg.CORBA.BAD_PARAM(
                "Unsupported Naming Service target address: " + targetAddress.discriminator(),
                0,
                CompletionStatus.COMPLETED_NO);
      };
    }

    private static byte[] objectKeyFromReferenceAddr(GiopTargetAddress targetAddress) {
      int selectedIndex;
      try {
        selectedIndex = Math.toIntExact(targetAddress.selectedProfileIndex());
      } catch (ArithmeticException exception) {
        throw new org.omg.CORBA.BAD_PARAM(
            "ReferenceAddr selected profile index is too large",
            exception,
            0,
            CompletionStatus.COMPLETED_NO);
      }
      if (selectedIndex < 0 || selectedIndex >= targetAddress.ior().profiles().size()) {
        throw new org.omg.CORBA.BAD_PARAM(
            "ReferenceAddr selected profile index is out of range",
            0,
            CompletionStatus.COMPLETED_NO);
      }
      return objectKey(targetAddress.ior().profiles().get(selectedIndex));
    }

    private static byte[] objectKey(TaggedProfile profile) {
      IiopProfile iiopProfile =
          profile
              .internetIopProfile()
              .orElseThrow(
                  () ->
                      new org.omg.CORBA.BAD_PARAM(
                          "Naming Service target profile is not TAG_INTERNET_IOP",
                          0,
                          CompletionStatus.COMPLETED_NO));
      return iiopProfile.objectKey().octets();
    }

    private Ior ior(ObjectKey objectKey) {
      IiopEndpoint currentEndpoint = Objects.requireNonNull(endpoint, "endpoint");
      IiopProfile profile =
          new IiopProfile(
              IiopVersion.V1_2,
              currentEndpoint.host(),
              currentEndpoint.port(),
              objectKey,
              List.of());
      return new Ior(NAMING_CONTEXT_REPOSITORY_ID, List.of(TaggedProfile.internetIop(profile)));
    }

    private ObjectKey rootObjectKey() {
      return rootKey;
    }

    private ObjectKey contextObjectKey(String contextId) {
      return persistenceOptions == null
          ? asciiObjectKey(NAME_SERVICE_KEY + "/" + contextId)
          : durableContextObjectKey(contextId);
    }

    private ObjectKey durableContextObjectKey(String contextId) {
      String orbId = persistenceOptions.orbIdentity().requireDurableOrbId();
      DurableObjectKey key =
          new DurableObjectKey(
              orbId,
              List.of("RootPOA", "NameService"),
              contextId.getBytes(StandardCharsets.US_ASCII),
              0);
      return new ObjectKey(key.encode());
    }

    private void persist() {
      if (persistenceStore != null) {
        persistenceStore.save(snapshot());
      }
    }

    private byte[] mutate(Supplier<byte[]> operation) {
      if (persistenceStore == null) {
        return operation.get();
      }
      NamingPersistenceStore.StoredState before = snapshot();
      try {
        byte[] result = operation.get();
        persist();
        return result;
      } catch (RuntimeException exception) {
        restore(before);
        throw exception;
      }
    }

    private NamingPersistenceStore.StoredState snapshot() {
      List<NamingPersistenceStore.StoredContext> storedContexts =
          contexts.entrySet().stream()
              .sorted(Comparator.comparing(entry -> entry.getKey().toHex()))
              .map(
                  entry ->
                      new NamingPersistenceStore.StoredContext(
                          ior(entry.getKey()),
                          entry.getValue().destroyed(),
                          entry.getValue().storedBindings()))
              .toList();
      return new NamingPersistenceStore.StoredState(nextContextId, storedContexts);
    }

    private void restore(NamingPersistenceStore.StoredState state) {
      contexts.clear();
      nextContextId = state.nextContextId();
      for (NamingPersistenceStore.StoredContext storedContext : state.contexts()) {
        contexts.put(
            objectKey(storedContext.ior()),
            new ContextState(storedContext.destroyed(), storedContext.bindings()));
      }
    }

    private void validateStoredContextTargets() {
      for (ContextState context : contexts.values()) {
        for (NamingPersistenceStore.StoredBinding binding : context.storedBindings()) {
          if (binding.target().kind() == RemoteNamingBindingTarget.Kind.CONTEXT
              && !contexts.containsKey(objectKey(binding.target().ior()))) {
            throw new NamingException(
                NamingDiagnosticCodes.INVALID_NAME,
                "Naming persistence store references a missing context");
          }
        }
      }
    }

    private static ObjectKey asciiObjectKey(String value) {
      return new ObjectKey(value.getBytes(StandardCharsets.US_ASCII));
    }

    private static ObjectKey objectKey(Ior ior) {
      return new ObjectKey(IiopObjectReference.fromIor(ior).objectKey());
    }
  }

  private static byte[] writeTarget(RemoteNamingBindingTarget target) {
    CdrWriter writer = CdrWriter.bigEndian();
    target.ior().writeTo(writer);
    return writer.toByteArray();
  }

  static RemoteNamingBindingTarget readTarget(byte[] body, RemoteNamingBindingTarget.Kind kind) {
    CdrReader reader = CdrReader.bigEndian(body);
    Ior ior = Ior.readFrom(reader, io.github.mundanej.mjo.ior.IorLimits.defaults());
    requireFullyRead(reader);
    return new RemoteNamingBindingTarget(kind, ior);
  }

  private static byte[] writeBindings(List<RemoteNamingBinding> bindings) {
    CdrWriter writer = CdrWriter.bigEndian();
    writer.writeSequenceLength(bindings.size());
    for (RemoteNamingBinding binding : bindings) {
      writeNameComponent(writer, binding.name());
      writer.writeUnsignedLong(
          binding.target().kind() == RemoteNamingBindingTarget.Kind.OBJECT ? 0 : 1);
    }
    Ior.nullReference().writeTo(writer);
    return writer.toByteArray();
  }

  static List<RemoteNamingBinding> readBindings(byte[] body) {
    CdrReader reader = CdrReader.bigEndian(body);
    int count = reader.readSequenceLength();
    List<RemoteNamingBinding> bindings = new ArrayList<>(count);
    for (int index = 0; index < count; index++) {
      NameComponent name = readNameComponent(reader);
      long bindingType = reader.readUnsignedLong();
      RemoteNamingBindingTarget.Kind kind =
          bindingType == 0
              ? RemoteNamingBindingTarget.Kind.OBJECT
              : RemoteNamingBindingTarget.Kind.CONTEXT;
      bindings.add(
          new RemoteNamingBinding(name, new RemoteNamingBindingTarget(kind, Ior.nullReference())));
    }
    Ior.readFrom(reader, io.github.mundanej.mjo.ior.IorLimits.defaults());
    requireFullyRead(reader);
    return List.copyOf(bindings);
  }

  static byte[] writeName(NamingName name) {
    CdrWriter writer = CdrWriter.bigEndian();
    writeName(writer, name);
    return writer.toByteArray();
  }

  private static NamingName readName(byte[] body) {
    CdrReader reader = CdrReader.bigEndian(body);
    NamingName name = readName(reader);
    requireFullyRead(reader);
    return name;
  }

  static byte[] writeBindingRequest(NamingName name, Ior ior) {
    CdrWriter writer = CdrWriter.bigEndian();
    writeName(writer, name);
    ior.writeTo(writer);
    return writer.toByteArray();
  }

  private static int readCount(byte[] body) {
    CdrReader reader = CdrReader.bigEndian(body);
    int count = reader.readLong();
    requireFullyRead(reader);
    if (count < 0) {
      throw new NamingException(NamingDiagnosticCodes.INVALID_NAME, "list count is negative");
    }
    return count;
  }

  static byte[] writeCount(int count) {
    return CdrWriter.bigEndian().writeLong(count).toByteArray();
  }

  private static void writeNameComponent(CdrWriter writer, NameComponent component) {
    writer.writeString(component.id()).writeString(component.kind());
  }

  private static void writeName(CdrWriter writer, NamingName name) {
    writer.writeSequenceLength(name.components().size());
    for (NameComponent component : name.components()) {
      writeNameComponent(writer, component);
    }
  }

  private static NamingName readName(CdrReader reader) {
    int componentCount = reader.readSequenceLength();
    List<NameComponent> components = new ArrayList<>(componentCount);
    for (int index = 0; index < componentCount; index++) {
      components.add(readNameComponent(reader));
    }
    return new NamingName(components);
  }

  private static NameComponent readNameComponent(CdrReader reader) {
    return new NameComponent(reader.readString(), reader.readString());
  }

  private static byte[] empty() {
    return new byte[0];
  }

  private static GiopReply systemExceptionReply(GiopRequest request, SystemException exception) {
    GiopSystemExceptionBody body =
        new GiopSystemExceptionBody(
            "IDL:omg.org/CORBA/" + exception.getClass().getSimpleName() + ":1.0",
            Integer.toUnsignedLong(exception.minor),
            completionStatus(exception.completed));
    return new GiopReply(
        replyHeader(request),
        request.requestId(),
        GiopReplyStatus.SYSTEM_EXCEPTION,
        request.serviceContexts(),
        body.toBytes(CdrByteOrder.BIG_ENDIAN));
  }

  private static SystemException systemException(NamingException exception) {
    int minor = codeMinor(exception.code());
    if (exception.code().equals(NamingDiagnosticCodes.NOT_FOUND)) {
      return new org.omg.CORBA.OBJECT_NOT_EXIST(
          exception.getMessage(), minor, CompletionStatus.COMPLETED_NO);
    }
    if (exception.code().equals(NamingDiagnosticCodes.NOT_EMPTY)
        || exception.code().equals(NamingDiagnosticCodes.DESTROYED)) {
      return new org.omg.CORBA.BAD_INV_ORDER(
          exception.getMessage(), minor, CompletionStatus.COMPLETED_NO);
    }
    return new org.omg.CORBA.BAD_PARAM(
        exception.getMessage(), minor, CompletionStatus.COMPLETED_NO);
  }

  static int codeMinor(DiagnosticCode code) {
    return Integer.parseInt(code.value().substring("NAM-".length()));
  }

  private static GiopCompletionStatus completionStatus(CompletionStatus status) {
    return switch (status) {
      case COMPLETED_YES -> GiopCompletionStatus.COMPLETED_YES;
      case COMPLETED_NO -> GiopCompletionStatus.COMPLETED_NO;
      case COMPLETED_MAYBE -> GiopCompletionStatus.COMPLETED_MAYBE;
    };
  }

  private static GiopHeader replyHeader(GiopRequest request) {
    return new GiopHeader(
        request.header().version(),
        request.header().littleEndian(),
        false,
        GiopMessageType.REPLY,
        0);
  }

  private static void requireFullyRead(CdrReader reader) {
    if (reader.remaining() != 0) {
      throw new NamingException(
          NamingDiagnosticCodes.INVALID_NAME,
          "Naming Service body has trailing octets: " + reader.remaining());
    }
  }

  private record BindingRequest(NamingName name, Ior ior) {

    private static BindingRequest read(byte[] body) {
      CdrReader reader = CdrReader.bigEndian(body);
      NamingName name = readName(reader);
      Ior ior = Ior.readFrom(reader, io.github.mundanej.mjo.ior.IorLimits.defaults());
      requireFullyRead(reader);
      return new BindingRequest(name, ior);
    }
  }

  private record ResolvedParent(ContextState context, NameComponent leaf) {}

  private static final class ContextState {

    private final Map<NameComponent, RemoteNamingBindingTarget> bindings = new LinkedHashMap<>();
    private boolean destroyed;

    private ContextState() {}

    private ContextState(
        boolean destroyed, List<NamingPersistenceStore.StoredBinding> storedBindings) {
      this.destroyed = destroyed;
      for (NamingPersistenceStore.StoredBinding binding : storedBindings) {
        if (bindings.put(binding.name(), binding.target()) != null) {
          throw new NamingException(
              NamingDiagnosticCodes.INVALID_NAME,
              "Naming persistence store contains duplicate bindings");
        }
      }
    }

    private void bind(NameComponent leaf, RemoteNamingBindingTarget target, boolean replace) {
      requireUsable();
      if (!replace && bindings.containsKey(leaf)) {
        throw new NamingException(
            NamingDiagnosticCodes.ALREADY_BOUND, "name component is already bound: " + leaf);
      }
      bindings.put(leaf, target);
    }

    private RemoteNamingBindingTarget resolve(NameComponent leaf) {
      requireUsable();
      RemoteNamingBindingTarget target = bindings.get(leaf);
      if (target == null) {
        throw new NamingException(
            NamingDiagnosticCodes.NOT_FOUND, "name component is not bound: " + leaf);
      }
      return target;
    }

    private void unbind(NameComponent leaf) {
      requireUsable();
      if (bindings.remove(leaf) == null) {
        throw new NamingException(
            NamingDiagnosticCodes.NOT_FOUND, "name component is not bound: " + leaf);
      }
    }

    private List<RemoteNamingBinding> list(int count) {
      requireUsable();
      return bindings.entrySet().stream()
          .limit(count)
          .map(entry -> new RemoteNamingBinding(entry.getKey(), entry.getValue()))
          .toList();
    }

    private void destroy() {
      requireUsable();
      if (!bindings.isEmpty()) {
        throw new NamingException(NamingDiagnosticCodes.NOT_EMPTY, "naming context is not empty");
      }
      destroyed = true;
    }

    private boolean destroyed() {
      return destroyed;
    }

    private List<NamingPersistenceStore.StoredBinding> storedBindings() {
      return bindings.entrySet().stream()
          .map(entry -> new NamingPersistenceStore.StoredBinding(entry.getKey(), entry.getValue()))
          .toList();
    }

    private void requireUsable() {
      if (destroyed) {
        throw new NamingException(
            NamingDiagnosticCodes.DESTROYED, "naming context has been destroyed");
      }
    }
  }

  private static String percentEncode(byte[] octets) {
    StringBuilder result = new StringBuilder(octets.length);
    for (byte octet : octets) {
      int value = octet & 0xFF;
      boolean unreserved =
          (value >= 'a' && value <= 'z')
              || (value >= 'A' && value <= 'Z')
              || (value >= '0' && value <= '9')
              || value == '.'
              || value == '_'
              || value == '-';
      if (unreserved) {
        result.append((char) value);
      } else {
        result.append('%');
        result.append(Character.toUpperCase(Character.forDigit((value >>> 4) & 0xF, 16)));
        result.append(Character.toUpperCase(Character.forDigit(value & 0xF, 16)));
      }
    }
    return result.toString();
  }
}
