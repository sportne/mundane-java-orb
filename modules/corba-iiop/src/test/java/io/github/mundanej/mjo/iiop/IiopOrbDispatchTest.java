package io.github.mundanej.mjo.iiop;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mundanej.mjo.cdr.CdrByteOrder;
import io.github.mundanej.mjo.cdr.CdrReader;
import io.github.mundanej.mjo.cdr.CdrWriter;
import io.github.mundanej.mjo.giop.GiopHeader;
import io.github.mundanej.mjo.giop.GiopMessageType;
import io.github.mundanej.mjo.giop.GiopReply;
import io.github.mundanej.mjo.giop.GiopReplyStatus;
import io.github.mundanej.mjo.giop.GiopRequest;
import io.github.mundanej.mjo.giop.GiopSystemExceptionBody;
import io.github.mundanej.mjo.giop.GiopTargetAddress;
import io.github.mundanej.mjo.giop.GiopUserExceptionBody;
import io.github.mundanej.mjo.ior.Ior;
import io.github.mundanej.mjo.ior.TaggedProfile;
import io.github.mundanej.mjo.modern.LocalInvocationRequest;
import io.github.mundanej.mjo.orb.LocalObjectReference;
import io.github.mundanej.mjo.orb.LocalOrb;
import io.github.mundanej.mjo.poa.Poa;
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
