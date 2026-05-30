package io.github.mundanej.mjo.iiop;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjo.cdr.CdrByteOrder;
import io.github.mundanej.mjo.cdr.CdrReader;
import io.github.mundanej.mjo.cdr.CdrWriter;
import io.github.mundanej.mjo.giop.GiopHeader;
import io.github.mundanej.mjo.giop.GiopMessageType;
import io.github.mundanej.mjo.giop.GiopReply;
import io.github.mundanej.mjo.giop.GiopReplyStatus;
import io.github.mundanej.mjo.giop.GiopRequest;
import io.github.mundanej.mjo.giop.GiopServiceContext;
import io.github.mundanej.mjo.giop.GiopSystemExceptionBody;
import io.github.mundanej.mjo.giop.GiopTargetAddress;
import io.github.mundanej.mjo.giop.GiopUserExceptionBody;
import io.github.mundanej.mjo.interceptors.ClientRequestContext;
import io.github.mundanej.mjo.interceptors.PortableClientRequestInterceptor;
import io.github.mundanej.mjo.interceptors.PortableInterceptorRegistry;
import io.github.mundanej.mjo.interceptors.PortableServerRequestInterceptor;
import io.github.mundanej.mjo.interceptors.ServerRequestContext;
import io.github.mundanej.mjo.ior.Ior;
import io.github.mundanej.mjo.ior.IorCodeSetComponent;
import io.github.mundanej.mjo.ior.StringifiedIor;
import io.github.mundanej.mjo.ior.TaggedProfile;
import io.github.mundanej.mjo.modern.LocalInvocationRequest;
import io.github.mundanej.mjo.orb.DurableObjectKey;
import io.github.mundanej.mjo.orb.LocalObjectReference;
import io.github.mundanej.mjo.orb.LocalOrb;
import io.github.mundanej.mjo.orb.OrbIdentity;
import io.github.mundanej.mjo.poa.Poa;
import io.github.mundanej.mjo.poa.PoaPolicySet;
import io.github.mundanej.mjo.poa.PoaServantDispatcher;
import io.github.mundanej.mjo.repositoryid.RepositoryId;
import io.github.mundanej.mjo.typecode.IdlGeneratedTypeDescriptor;
import io.github.mundanej.mjo.typecode.IdlOperationDescriptor;
import io.github.mundanej.mjo.typecode.IdlParameterDescriptor;
import io.github.mundanej.mjo.typecode.IdlParameterMode;
import io.github.mundanej.mjo.typecode.IdlTypeKind;
import io.github.mundanej.mjo.typecode.IdlTypeReference;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.omg.CORBA.SystemException;

/** Local integration tests for network IIOP dispatch through ORB and POA references. */
@Tag("unit")
final class IiopOrbDispatchTest {

  private static final IdlTypeReference STRING_TYPE =
      new IdlTypeReference(IdlTypeKind.PRIMITIVE, "string", "java.lang.String", Optional.empty());
  private static final IdlTypeReference GREET_FAILURE =
      new IdlTypeReference(
          IdlTypeKind.EXCEPTION,
          "::hello::GreetFailure",
          IiopOrbDispatchTest.GreetFailure.class.getName(),
          Optional.of(RepositoryId.parse("IDL:hello/GreetFailure:1.0")));
  private static final IdlOperationDescriptor GREET =
      new IdlOperationDescriptor(
          "greet",
          STRING_TYPE,
          List.of(new IdlParameterDescriptor("name", IdlParameterMode.IN, STRING_TYPE)),
          List.of(GREET_FAILURE));
  private static final IdlGeneratedTypeDescriptor GREETER_DESCRIPTOR =
      new IdlGeneratedTypeDescriptor(
          IdlTypeKind.INTERFACE,
          "::hello::Greeter",
          "hello.Greeter",
          RepositoryId.parse("IDL:hello/Greeter:1.0"),
          List.of(),
          List.of(),
          List.of(GREET));
  private static final IiopInvocationCodec STRING_CODEC = new StringOperationCodec();

  @Test
  void poaActivatedServantDispatchesOverLoopbackIiop() {
    LocalOrb orb = LocalOrb.create();
    Poa poa = Poa.createRoot(orb);
    LocalObjectReference<Greeter> localReference =
        poa.activateServant(
            Greeter.class, GREETER_DESCRIPTOR, new GreeterServant(), new GreeterDispatcher());
    IiopOrbServerHandler handler =
        IiopOrbServerHandler.builder(orb)
            .bind(localReference, List.of(new IiopOperationBinding(GREET, STRING_CODEC)))
            .build();

    try (IiopServer server =
        IiopServer.bind(IiopEndpoint.loopback(0), IiopOptions.defaults(), handler)) {
      IiopObjectReference networkReference =
          IiopObjectReference.fromLocal(server.endpoint(), localReference);
      try (IiopOrbClient client = IiopOrbClient.connect(networkReference, IiopOptions.defaults())) {
        assertEquals("Hello Ada", client.invoke(GREET, STRING_CODEC, List.of("Ada")));
      }
      assertEquals("IDL:hello/Greeter:1.0", networkReference.ior().typeId());
      assertArrayEquals(
          localReference.objectId().getBytes(java.nio.charset.StandardCharsets.US_ASCII),
          networkReference.objectKey());
    }
  }

  @Test
  void unknownOperationMapsToDeterministicSystemExceptionReply() {
    LocalOrb orb = LocalOrb.create();
    Poa poa = Poa.createRoot(orb);
    LocalObjectReference<Greeter> localReference =
        poa.activateServant(
            Greeter.class, GREETER_DESCRIPTOR, new GreeterServant(), new GreeterDispatcher());
    IiopOrbServerHandler handler =
        IiopOrbServerHandler.builder(orb)
            .bind(localReference, List.of(new IiopOperationBinding(GREET, STRING_CODEC)))
            .build();
    try (IiopServer server =
            IiopServer.bind(IiopEndpoint.loopback(0), IiopOptions.defaults(), handler);
        IiopClient client = IiopClient.connect(server.endpoint(), IiopOptions.defaults())) {
      GiopReply reply =
          client.invoke(
              new GiopRequest(
                  GiopHeader.forType(GiopMessageType.REQUEST),
                  3,
                  3,
                  localReference.objectId().getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                  "missing",
                  List.of(),
                  new byte[0]));

      assertEquals(GiopReplyStatus.SYSTEM_EXCEPTION, reply.replyStatus());
      GiopSystemExceptionBody body =
          GiopSystemExceptionBody.fromBytes(CdrByteOrder.BIG_ENDIAN, reply.body());
      assertEquals("IDL:omg.org/CORBA/BAD_OPERATION:1.0", body.repositoryId());
    }
  }

  @Test
  void clientTurnsSystemExceptionRepliesIntoSystemExceptions() {
    LocalOrb orb = LocalOrb.create();
    Poa poa = Poa.createRoot(orb);
    LocalObjectReference<Greeter> localReference =
        poa.activateServant(
            Greeter.class, GREETER_DESCRIPTOR, new GreeterServant(), new GreeterDispatcher());
    IiopOrbServerHandler handler =
        IiopOrbServerHandler.builder(orb)
            .bind(localReference, List.of(new IiopOperationBinding(GREET, STRING_CODEC)))
            .build();

    try (IiopServer server =
        IiopServer.bind(IiopEndpoint.loopback(0), IiopOptions.defaults(), handler)) {
      IiopObjectReference networkReference =
          IiopObjectReference.fromLocal(server.endpoint(), localReference);
      try (IiopOrbClient client = IiopOrbClient.connect(networkReference, IiopOptions.defaults())) {
        assertThrows(
            SystemException.class,
            () -> client.invoke(GREET, STRING_CODEC, List.of("system-exception")));
      }
    }
  }

  @Test
  void clientPreservesUnsignedSystemExceptionMinorCode() {
    try (IiopServer server =
        IiopServer.bind(
            IiopEndpoint.loopback(0),
            IiopOptions.defaults(),
            request ->
                new GiopReply(
                    new GiopHeader(
                        request.header().version(),
                        request.header().littleEndian(),
                        false,
                        GiopMessageType.REPLY,
                        0),
                    request.requestId(),
                    GiopReplyStatus.SYSTEM_EXCEPTION,
                    List.of(),
                    new GiopSystemExceptionBody(
                            "IDL:omg.org/CORBA/UNKNOWN:1.0",
                            0x8000_0000L,
                            io.github.mundanej.mjo.giop.GiopCompletionStatus.COMPLETED_NO)
                        .toBytes(CdrByteOrder.BIG_ENDIAN)))) {
      IiopObjectReference networkReference =
          new IiopObjectReference(
              new Ior("IDL:hello/Greeter:1.0", List.of()),
              server.endpoint(),
              "local-1".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
      try (IiopOrbClient client = IiopOrbClient.connect(networkReference, IiopOptions.defaults())) {
        SystemException exception =
            assertThrows(
                SystemException.class, () -> client.invoke(GREET, STRING_CODEC, List.of("Ada")));
        assertEquals(Integer.MIN_VALUE, exception.minor);
      }
    }
  }

  @Test
  void profileAndReferenceTargetAddressesDispatchToBoundObjectKey() {
    LocalOrb orb = LocalOrb.create();
    Poa poa = Poa.createRoot(orb);
    LocalObjectReference<Greeter> localReference =
        poa.activateServant(
            Greeter.class, GREETER_DESCRIPTOR, new GreeterServant(), new GreeterDispatcher());
    IiopOrbServerHandler handler =
        IiopOrbServerHandler.builder(orb)
            .bind(localReference, List.of(new IiopOperationBinding(GREET, STRING_CODEC)))
            .build();

    try (IiopServer server =
            IiopServer.bind(IiopEndpoint.loopback(0), IiopOptions.defaults(), handler);
        IiopClient client = IiopClient.connect(server.endpoint(), IiopOptions.defaults())) {
      IiopObjectReference networkReference =
          IiopObjectReference.fromLocal(server.endpoint(), localReference);
      TaggedProfile profile = networkReference.ior().profiles().get(0);

      assertEquals(
          "Hello Profile",
          decodedStringReply(
              client.invoke(request(5, GiopTargetAddress.profileAddr(profile), "Profile"))));
      assertEquals(
          "Hello Reference",
          decodedStringReply(
              client.invoke(
                  request(
                      6,
                      GiopTargetAddress.referenceAddr(0, networkReference.ior()),
                      "Reference"))));
    }
  }

  @Test
  void persistentTargetAddressesPreserveDurableObjectKeys() {
    LocalOrb orb = LocalOrb.create(OrbIdentity.durable("target-orb"));
    Poa poa = Poa.createRoot(orb, persistentUserIdPolicy());
    LocalObjectReference<Greeter> localReference =
        poa.activateServantWithId(
            "alpha",
            Greeter.class,
            GREETER_DESCRIPTOR,
            new GreeterServant(),
            new GreeterDispatcher());
    IiopOrbServerHandler handler =
        IiopOrbServerHandler.builder(orb)
            .bind(localReference, List.of(new IiopOperationBinding(GREET, STRING_CODEC)))
            .build();

    try (IiopServer server =
            IiopServer.bind(IiopEndpoint.loopback(0), IiopOptions.defaults(), handler);
        IiopClient client = IiopClient.connect(server.endpoint(), IiopOptions.defaults())) {
      IiopObjectReference networkReference =
          IiopObjectReference.fromLocal(server.endpoint(), localReference);
      TaggedProfile profile = networkReference.ior().profiles().get(0);
      byte[] durableKey = localReference.durableObjectKey().orElseThrow().encode();

      assertArrayEquals(durableKey, networkReference.objectKey());
      assertEquals(
          "Hello Key",
          decodedStringReply(
              client.invoke(request(51, GiopTargetAddress.keyAddr(durableKey), "Key"))));
      assertEquals(
          "Hello Profile",
          decodedStringReply(
              client.invoke(request(52, GiopTargetAddress.profileAddr(profile), "Profile"))));
      assertEquals(
          "Hello Reference",
          decodedStringReply(
              client.invoke(
                  request(
                      53,
                      GiopTargetAddress.referenceAddr(0, networkReference.ior()),
                      "Reference"))));
    }
  }

  @Test
  void persistentStringifiedIorRoutesAfterRestartSimulation() {
    IiopEndpoint endpoint;
    String stringifiedIor;
    DurableObjectKey durableKey;
    LocalOrb firstOrb = LocalOrb.create(OrbIdentity.durable("restart-iiop-orb"));
    Poa firstPoa = Poa.createRoot(firstOrb, persistentUserIdPolicy());
    LocalObjectReference<Greeter> firstReference =
        firstPoa.activateServantWithId(
            "alpha",
            Greeter.class,
            GREETER_DESCRIPTOR,
            new GreeterServant(),
            new GreeterDispatcher());
    IiopOrbServerHandler firstHandler =
        IiopOrbServerHandler.builder(firstOrb)
            .bind(firstReference, List.of(new IiopOperationBinding(GREET, STRING_CODEC)))
            .build();

    try (IiopServer firstServer =
        IiopServer.bind(IiopEndpoint.loopback(0), IiopOptions.defaults(), firstHandler)) {
      IiopObjectReference firstNetworkReference =
          IiopObjectReference.fromLocal(firstServer.endpoint(), firstReference);
      endpoint = firstServer.endpoint();
      stringifiedIor = StringifiedIor.format(firstNetworkReference.ior());
      durableKey = firstReference.durableObjectKey().orElseThrow();

      assertArrayEquals(durableKey.encode(), firstNetworkReference.objectKey());
    }

    LocalOrb secondOrb = LocalOrb.create(OrbIdentity.durable("restart-iiop-orb"));
    Poa secondPoa = Poa.createRoot(secondOrb, persistentUserIdPolicy());
    LocalObjectReference<Greeter> secondReference =
        secondPoa.activateServantWithId(
            "alpha",
            Greeter.class,
            GREETER_DESCRIPTOR,
            new GreeterServant(),
            new GreeterDispatcher());
    IiopOrbServerHandler secondHandler =
        IiopOrbServerHandler.builder(secondOrb)
            .bind(secondReference, List.of(new IiopOperationBinding(GREET, STRING_CODEC)))
            .build();

    try (IiopServer secondServer =
        IiopServer.bind(endpoint, IiopOptions.defaults(), secondHandler)) {
      assertEquals(endpoint, secondServer.endpoint());
      IiopObjectReference parsedReference =
          IiopObjectReference.fromIor(StringifiedIor.parse(stringifiedIor));
      try (IiopOrbClient client = IiopOrbClient.connect(parsedReference, IiopOptions.defaults())) {
        assertEquals("Hello Ada", client.invoke(GREET, STRING_CODEC, List.of("Ada")));
      }
    }
  }

  @Test
  void unknownObjectKeyAndInvalidTargetAddressProduceSystemExceptionReplies() {
    LocalOrb orb = LocalOrb.create();
    Poa poa = Poa.createRoot(orb);
    LocalObjectReference<Greeter> localReference =
        poa.activateServant(
            Greeter.class, GREETER_DESCRIPTOR, new GreeterServant(), new GreeterDispatcher());
    IiopOrbServerHandler handler =
        IiopOrbServerHandler.builder(orb)
            .bind(localReference, List.of(new IiopOperationBinding(GREET, STRING_CODEC)))
            .build();

    try (IiopServer server =
            IiopServer.bind(IiopEndpoint.loopback(0), IiopOptions.defaults(), handler);
        IiopClient client = IiopClient.connect(server.endpoint(), IiopOptions.defaults())) {
      GiopReply unknownKey =
          client.invoke(request(7, GiopTargetAddress.keyAddr(new byte[] {9}), "Ada"));
      assertEquals(GiopReplyStatus.SYSTEM_EXCEPTION, unknownKey.replyStatus());
      assertEquals(
          "IDL:omg.org/CORBA/OBJECT_NOT_EXIST:1.0",
          GiopSystemExceptionBody.fromBytes(CdrByteOrder.BIG_ENDIAN, unknownKey.body())
              .repositoryId());

      GiopReply malformedDurableKey =
          client.invoke(
              request(77, GiopTargetAddress.keyAddr(new byte[] {'M', 'J', 'O', 'K', 1}), "Ada"));
      assertEquals(GiopReplyStatus.SYSTEM_EXCEPTION, malformedDurableKey.replyStatus());
      assertEquals(
          "IDL:omg.org/CORBA/BAD_PARAM:1.0",
          GiopSystemExceptionBody.fromBytes(CdrByteOrder.BIG_ENDIAN, malformedDurableKey.body())
              .repositoryId());

      GiopReply invalidReference =
          client.invoke(
              request(
                  8,
                  GiopTargetAddress.referenceAddr(
                      1, new Ior("IDL:hello/Greeter:1.0", networkProfiles(server, localReference))),
                  "Ada"));
      assertEquals(GiopReplyStatus.SYSTEM_EXCEPTION, invalidReference.replyStatus());
      assertEquals(
          "IDL:omg.org/CORBA/BAD_PARAM:1.0",
          GiopSystemExceptionBody.fromBytes(CdrByteOrder.BIG_ENDIAN, invalidReference.body())
              .repositoryId());
    }
  }

  @Test
  void declaredUserExceptionBecomesUserExceptionReply() {
    LocalOrb orb = LocalOrb.create();
    Poa poa = Poa.createRoot(orb);
    LocalObjectReference<Greeter> localReference =
        poa.activateServant(
            Greeter.class, GREETER_DESCRIPTOR, new GreeterServant(), new GreeterDispatcher());
    IiopOrbServerHandler handler =
        IiopOrbServerHandler.builder(orb)
            .bind(localReference, List.of(new IiopOperationBinding(GREET, STRING_CODEC)))
            .build();

    try (IiopServer server =
            IiopServer.bind(IiopEndpoint.loopback(0), IiopOptions.defaults(), handler);
        IiopClient client = IiopClient.connect(server.endpoint(), IiopOptions.defaults())) {
      GiopReply reply =
          client.invoke(
              request(
                  9,
                  GiopTargetAddress.keyAddr(
                      localReference
                          .objectId()
                          .getBytes(java.nio.charset.StandardCharsets.US_ASCII)),
                  "user-exception"));

      assertEquals(GiopReplyStatus.USER_EXCEPTION, reply.replyStatus());
      assertEquals(
          "IDL:hello/GreetFailure:1.0",
          GiopUserExceptionBody.fromBytes(CdrByteOrder.BIG_ENDIAN, reply.body()).repositoryId());
    }
  }

  @Test
  void clientDecodesDeclaredUserExceptionPayloadWithOperationCodec() {
    LocalOrb orb = LocalOrb.create();
    Poa poa = Poa.createRoot(orb);
    LocalObjectReference<Greeter> localReference =
        poa.activateServant(
            Greeter.class, GREETER_DESCRIPTOR, new GreeterServant(), new GreeterDispatcher());
    IiopOrbServerHandler handler =
        IiopOrbServerHandler.builder(orb)
            .bind(localReference, List.of(new IiopOperationBinding(GREET, STRING_CODEC)))
            .build();

    try (IiopServer server =
        IiopServer.bind(IiopEndpoint.loopback(0), IiopOptions.defaults(), handler)) {
      IiopObjectReference networkReference =
          IiopObjectReference.fromLocal(server.endpoint(), localReference);
      try (IiopOrbClient client = IiopOrbClient.connect(networkReference, IiopOptions.defaults())) {
        UserReplyException exception =
            assertThrows(
                UserReplyException.class,
                () -> client.invoke(GREET, STRING_CODEC, List.of("user-exception")));

        assertEquals("IDL:hello/GreetFailure:1.0", exception.repositoryId());
        assertEquals("bad greeting", exception.payload());
      }
    }
  }

  @Test
  void portableInterceptorsPropagateServiceContextsOverOrbIiopPath() {
    LocalOrb orb = LocalOrb.create();
    Poa poa = Poa.createRoot(orb);
    LocalObjectReference<Greeter> localReference =
        poa.activateServant(
            Greeter.class, GREETER_DESCRIPTOR, new GreeterServant(), new GreeterDispatcher());
    GiopServiceContext requestContext = new GiopServiceContext(100, new byte[] {1});
    GiopServiceContext replyContext = new GiopServiceContext(200, new byte[] {2});
    AtomicReference<List<GiopServiceContext>> clientReplyContexts = new AtomicReference<>();
    AtomicReference<List<GiopServiceContext>> serverRequestContexts = new AtomicReference<>();
    PortableInterceptorRegistry registry =
        PortableInterceptorRegistry.builder()
            .addClient(
                new PortableClientRequestInterceptor() {
                  @Override
                  public String name() {
                    return "client";
                  }

                  @Override
                  public void sendRequest(ClientRequestContext context) {
                    context.addRequestServiceContext(requestContext, false);
                  }

                  @Override
                  public void receiveReply(ClientRequestContext context) {
                    clientReplyContexts.set(context.replyServiceContexts());
                  }
                })
            .addServer(
                new PortableServerRequestInterceptor() {
                  @Override
                  public String name() {
                    return "server";
                  }

                  @Override
                  public void receiveRequestServiceContexts(ServerRequestContext context) {
                    serverRequestContexts.set(context.requestServiceContexts());
                  }

                  @Override
                  public void sendReply(ServerRequestContext context) {
                    context.addReplyServiceContext(replyContext, false);
                  }
                })
            .build();
    IiopOrbServerHandler handler =
        IiopOrbServerHandler.builder(orb)
            .interceptors(registry)
            .bind(localReference, List.of(new IiopOperationBinding(GREET, STRING_CODEC)))
            .build();

    try (IiopServer server =
        IiopServer.bind(IiopEndpoint.loopback(0), IiopOptions.defaults(), handler)) {
      IiopObjectReference networkReference =
          IiopObjectReference.fromLocal(server.endpoint(), localReference);
      try (IiopOrbClient client =
          IiopOrbClient.connect(networkReference, IiopOptions.defaults(), registry)) {
        assertEquals("Hello Ada", client.invoke(GREET, STRING_CODEC, List.of("Ada")));
      }
    }

    assertEquals(List.of(requestContext), serverRequestContexts.get());
    assertEquals(List.of(replyContext), clientReplyContexts.get());
  }

  @Test
  void clientReplyInterceptorFailureDoesNotRunExceptionCallback() {
    LocalOrb orb = LocalOrb.create();
    Poa poa = Poa.createRoot(orb);
    LocalObjectReference<Greeter> localReference =
        poa.activateServant(
            Greeter.class, GREETER_DESCRIPTOR, new GreeterServant(), new GreeterDispatcher());
    AtomicBoolean exceptionCallbackInvoked = new AtomicBoolean();
    PortableInterceptorRegistry registry =
        PortableInterceptorRegistry.builder()
            .addClient(
                new PortableClientRequestInterceptor() {
                  @Override
                  public String name() {
                    return "client";
                  }

                  @Override
                  public void receiveReply(ClientRequestContext context) {
                    throw new IllegalStateException("reply callback failed");
                  }

                  @Override
                  public void receiveException(ClientRequestContext context) {
                    exceptionCallbackInvoked.set(true);
                  }
                })
            .build();
    IiopOrbServerHandler handler =
        IiopOrbServerHandler.builder(orb)
            .bind(localReference, List.of(new IiopOperationBinding(GREET, STRING_CODEC)))
            .build();

    try (IiopServer server =
        IiopServer.bind(IiopEndpoint.loopback(0), IiopOptions.defaults(), handler)) {
      IiopObjectReference networkReference =
          IiopObjectReference.fromLocal(server.endpoint(), localReference);
      try (IiopOrbClient client =
          IiopOrbClient.connect(networkReference, IiopOptions.defaults(), registry)) {
        assertThrows(
            io.github.mundanej.mjo.interceptors.InterceptorException.class,
            () -> client.invoke(GREET, STRING_CODEC, List.of("Ada")));
      }
    }

    assertEquals(false, exceptionCallbackInvoked.get());
  }

  @Test
  void clientExceptionInterceptorRunsForLocalEncodeFailure() {
    AtomicBoolean exceptionCallbackInvoked = new AtomicBoolean();
    PortableInterceptorRegistry registry =
        PortableInterceptorRegistry.builder()
            .addClient(
                new PortableClientRequestInterceptor() {
                  @Override
                  public String name() {
                    return "client";
                  }

                  @Override
                  public void receiveException(ClientRequestContext context) {
                    exceptionCallbackInvoked.set(true);
                  }
                })
            .build();
    IiopInvocationCodec failingCodec =
        new IiopInvocationCodec() {
          @Override
          public List<Object> decodeArguments(
              IdlOperationDescriptor operation, byte[] requestBody) {
            return STRING_CODEC.decodeArguments(operation, requestBody);
          }

          @Override
          public byte[] encodeArguments(IdlOperationDescriptor operation, List<Object> arguments) {
            throw new IllegalStateException("encode failed");
          }

          @Override
          public byte[] encodeReturnValue(IdlOperationDescriptor operation, Object value) {
            return STRING_CODEC.encodeReturnValue(operation, value);
          }

          @Override
          public Object decodeReturnValue(IdlOperationDescriptor operation, byte[] replyBody) {
            return STRING_CODEC.decodeReturnValue(operation, replyBody);
          }

          @Override
          public byte[] encodeUserException(
              io.github.mundanej.mjo.orb.LocalInvocationUserException exception) {
            return STRING_CODEC.encodeUserException(exception);
          }

          @Override
          public RuntimeException decodeUserException(
              IdlOperationDescriptor operation, String repositoryId, byte[] exceptionBody) {
            return STRING_CODEC.decodeUserException(operation, repositoryId, exceptionBody);
          }
        };

    try (IiopServer server =
        IiopServer.bind(
            IiopEndpoint.loopback(0),
            IiopOptions.defaults(),
            request ->
                new GiopReply(
                    new GiopHeader(
                        request.header().version(),
                        request.header().littleEndian(),
                        false,
                        GiopMessageType.REPLY,
                        0),
                    request.requestId(),
                    GiopReplyStatus.NO_EXCEPTION,
                    List.of(),
                    new byte[0]))) {
      IiopObjectReference networkReference =
          new IiopObjectReference(
              new Ior("IDL:hello/Greeter:1.0", List.of()),
              server.endpoint(),
              "local-1".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
      try (IiopOrbClient client =
          IiopOrbClient.connect(networkReference, IiopOptions.defaults(), registry)) {
        assertThrows(
            IllegalStateException.class, () -> client.invoke(GREET, failingCodec, List.of("Ada")));
      }
    }

    assertEquals(true, exceptionCallbackInvoked.get());
  }

  @Test
  void serverExceptionInterceptorFailureReturnsDeterministicSystemException() {
    LocalOrb orb = LocalOrb.create();
    PortableInterceptorRegistry registry =
        PortableInterceptorRegistry.builder()
            .addServer(
                new PortableServerRequestInterceptor() {
                  @Override
                  public String name() {
                    return "server";
                  }

                  @Override
                  public void sendException(ServerRequestContext context) {
                    throw new IllegalStateException("exception callback failed");
                  }
                })
            .build();
    IiopOrbServerHandler handler = IiopOrbServerHandler.builder(orb).interceptors(registry).build();

    try (IiopServer server =
            IiopServer.bind(IiopEndpoint.loopback(0), IiopOptions.defaults(), handler);
        IiopClient client = IiopClient.connect(server.endpoint(), IiopOptions.defaults())) {
      GiopReply reply =
          client.invoke(
              new GiopRequest(
                  GiopHeader.forType(GiopMessageType.REQUEST),
                  7,
                  3,
                  "missing".getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                  "missing",
                  List.of(new GiopServiceContext(100, new byte[] {1})),
                  new byte[0]));

      assertEquals(GiopReplyStatus.SYSTEM_EXCEPTION, reply.replyStatus());
      assertEquals(List.of(), reply.serviceContexts());
      GiopSystemExceptionBody body =
          GiopSystemExceptionBody.fromBytes(CdrByteOrder.BIG_ENDIAN, reply.body());
      assertEquals("IDL:omg.org/CORBA/UNKNOWN:1.0", body.repositoryId());
    }
  }

  @Test
  void serverReplyCallbackDoesNotRunWhenReturnEncodingFails() {
    LocalOrb orb = LocalOrb.create();
    Poa poa = Poa.createRoot(orb);
    LocalObjectReference<Greeter> localReference =
        poa.activateServant(
            Greeter.class, GREETER_DESCRIPTOR, new GreeterServant(), new GreeterDispatcher());
    AtomicBoolean replyCallbackInvoked = new AtomicBoolean();
    AtomicBoolean exceptionCallbackInvoked = new AtomicBoolean();
    PortableInterceptorRegistry registry =
        PortableInterceptorRegistry.builder()
            .addServer(
                new PortableServerRequestInterceptor() {
                  @Override
                  public String name() {
                    return "server";
                  }

                  @Override
                  public void sendReply(ServerRequestContext context) {
                    replyCallbackInvoked.set(true);
                  }

                  @Override
                  public void sendException(ServerRequestContext context) {
                    exceptionCallbackInvoked.set(true);
                  }
                })
            .build();
    IiopInvocationCodec failingCodec =
        new IiopInvocationCodec() {
          @Override
          public List<Object> decodeArguments(
              IdlOperationDescriptor operation, byte[] requestBody) {
            return STRING_CODEC.decodeArguments(operation, requestBody);
          }

          @Override
          public byte[] encodeArguments(IdlOperationDescriptor operation, List<Object> arguments) {
            return STRING_CODEC.encodeArguments(operation, arguments);
          }

          @Override
          public byte[] encodeReturnValue(IdlOperationDescriptor operation, Object value) {
            throw new IllegalStateException("return encode failed");
          }

          @Override
          public Object decodeReturnValue(IdlOperationDescriptor operation, byte[] replyBody) {
            return STRING_CODEC.decodeReturnValue(operation, replyBody);
          }

          @Override
          public byte[] encodeUserException(
              io.github.mundanej.mjo.orb.LocalInvocationUserException exception) {
            return STRING_CODEC.encodeUserException(exception);
          }

          @Override
          public RuntimeException decodeUserException(
              IdlOperationDescriptor operation, String repositoryId, byte[] exceptionBody) {
            return STRING_CODEC.decodeUserException(operation, repositoryId, exceptionBody);
          }
        };
    IiopOrbServerHandler handler =
        IiopOrbServerHandler.builder(orb)
            .interceptors(registry)
            .bind(localReference, List.of(new IiopOperationBinding(GREET, failingCodec)))
            .build();

    try (IiopServer server =
            IiopServer.bind(IiopEndpoint.loopback(0), IiopOptions.defaults(), handler);
        IiopClient client = IiopClient.connect(server.endpoint(), IiopOptions.defaults())) {
      GiopReply reply =
          client.invoke(
              new GiopRequest(
                  GiopHeader.forType(GiopMessageType.REQUEST),
                  8,
                  3,
                  localReference.objectId().getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                  "greet",
                  List.of(),
                  STRING_CODEC.encodeArguments(GREET, List.of("Ada"))));

      assertEquals(GiopReplyStatus.SYSTEM_EXCEPTION, reply.replyStatus());
      assertEquals(false, replyCallbackInvoked.get());
      assertEquals(true, exceptionCallbackInvoked.get());
    }
  }

  @Test
  void objectReferencesCopyObjectKeyAndRejectInvalidIors() {
    LocalOrb orb = LocalOrb.create();
    Poa poa = Poa.createRoot(orb);
    LocalObjectReference<Greeter> localReference =
        poa.activateServant(
            Greeter.class, GREETER_DESCRIPTOR, new GreeterServant(), new GreeterDispatcher());
    IiopObjectReference reference =
        IiopObjectReference.fromLocal(IiopEndpoint.loopback(9), localReference);
    IiopObjectReference decoded = IiopObjectReference.fromIor(reference.ior());
    byte[] objectKey = reference.objectKey();
    objectKey[0] = 0;

    assertEquals(reference, decoded);
    assertEquals(reference.hashCode(), decoded.hashCode());
    assertArrayEquals(
        localReference.objectId().getBytes(java.nio.charset.StandardCharsets.US_ASCII),
        reference.objectKey());
    assertTrue(
        reference.ior().profiles().stream()
            .flatMap(profile -> profile.internetIopProfile().stream())
            .flatMap(profile -> profile.components().stream())
            .anyMatch(component -> component.equals(IorCodeSetComponent.defaults().toComponent())));
    assertThrows(
        IiopException.class,
        () -> IiopObjectReference.fromIor(new Ior("IDL:hello/Greeter:1.0", List.of())));
    assertThrows(IiopException.class, () -> IiopObjectReference.objectKeyFor(" "));
    assertThrows(IiopException.class, () -> IiopObjectReference.objectKeyFor("caf\u00e9"));
  }

  private static GiopRequest request(long requestId, GiopTargetAddress targetAddress, String name) {
    return new GiopRequest(
        GiopHeader.forType(GiopMessageType.REQUEST),
        requestId,
        3,
        targetAddress,
        "greet",
        List.of(),
        CdrWriter.bigEndian().writeString(name).toByteArray());
  }

  private static List<TaggedProfile> networkProfiles(
      IiopServer server, LocalObjectReference<Greeter> localReference) {
    return IiopObjectReference.fromLocal(server.endpoint(), localReference).ior().profiles();
  }

  private static PoaPolicySet persistentUserIdPolicy() {
    return new PoaPolicySet(
        PoaPolicySet.ThreadPolicy.ORB_CTRL_MODEL,
        PoaPolicySet.LifespanPolicy.PERSISTENT,
        PoaPolicySet.IdUniquenessPolicy.UNIQUE_ID,
        PoaPolicySet.IdAssignmentPolicy.USER_ID,
        PoaPolicySet.ServantRetentionPolicy.RETAIN,
        PoaPolicySet.RequestProcessingPolicy.USE_ACTIVE_OBJECT_MAP_ONLY,
        PoaPolicySet.ImplicitActivationPolicy.NO_IMPLICIT_ACTIVATION);
  }

  private static String decodedStringReply(GiopReply reply) {
    assertEquals(GiopReplyStatus.NO_EXCEPTION, reply.replyStatus());
    return CdrReader.bigEndian(reply.body()).readString();
  }

  private interface Greeter {
    String greet(String name) throws GreetFailure;
  }

  private static final class GreeterServant implements Greeter {

    @Override
    public String greet(String name) throws GreetFailure {
      if ("system-exception".equals(name)) {
        throw new org.omg.CORBA.BAD_PARAM(
            "bad name", 7, org.omg.CORBA.CompletionStatus.COMPLETED_NO);
      }
      if ("user-exception".equals(name)) {
        throw new GreetFailure("bad greeting");
      }
      return "Hello " + name;
    }
  }

  private static final class GreeterDispatcher implements PoaServantDispatcher<Greeter> {

    @Override
    public Object invoke(Greeter servant, LocalInvocationRequest request) throws Exception {
      return servant.greet((String) request.arguments().get(0));
    }
  }

  private static final class GreetFailure extends Exception {

    private static final long serialVersionUID = 1L;

    private GreetFailure(String message) {
      super(message);
    }
  }

  private static final class StringOperationCodec implements IiopInvocationCodec {

    @Override
    public List<Object> decodeArguments(IdlOperationDescriptor operation, byte[] requestBody) {
      return List.of(CdrReader.bigEndian(requestBody).readString());
    }

    @Override
    public byte[] encodeArguments(IdlOperationDescriptor operation, List<Object> arguments) {
      return CdrWriter.bigEndian().writeString((String) arguments.get(0)).toByteArray();
    }

    @Override
    public byte[] encodeReturnValue(IdlOperationDescriptor operation, Object value) {
      return CdrWriter.bigEndian().writeString((String) value).toByteArray();
    }

    @Override
    public Object decodeReturnValue(IdlOperationDescriptor operation, byte[] replyBody) {
      return CdrReader.bigEndian(replyBody).readString();
    }

    @Override
    public byte[] encodeUserException(
        io.github.mundanej.mjo.orb.LocalInvocationUserException exception) {
      return CdrWriter.bigEndian()
          .writeString(exception.userException().getMessage())
          .toByteArray();
    }

    @Override
    public RuntimeException decodeUserException(
        IdlOperationDescriptor operation, String repositoryId, byte[] exceptionBody) {
      return new UserReplyException(repositoryId, CdrReader.bigEndian(exceptionBody).readString());
    }
  }

  private static final class UserReplyException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String repositoryId;
    private final String payload;

    private UserReplyException(String repositoryId, String payload) {
      super(repositoryId + ": " + payload);
      this.repositoryId = repositoryId;
      this.payload = payload;
    }

    private String repositoryId() {
      return repositoryId;
    }

    private String payload() {
      return payload;
    }
  }
}
