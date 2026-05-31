package io.github.mundanej.mjo.nativeimage.smoke;

import io.github.mundanej.mjo.cdr.CdrReader;
import io.github.mundanej.mjo.cdr.CdrWriter;
import io.github.mundanej.mjo.giop.GiopHeader;
import io.github.mundanej.mjo.giop.GiopMessageType;
import io.github.mundanej.mjo.giop.GiopReply;
import io.github.mundanej.mjo.giop.GiopReplyStatus;
import io.github.mundanej.mjo.giop.GiopRequest;
import io.github.mundanej.mjo.giop.GiopTargetAddress;
import io.github.mundanej.mjo.iiop.IiopInvocationCodec;
import io.github.mundanej.mjo.iiop.IiopOperationBinding;
import io.github.mundanej.mjo.iiop.IiopOrbServerHandler;
import io.github.mundanej.mjo.modern.LocalInvocationDispatcher;
import io.github.mundanej.mjo.orb.DurableObjectKey;
import io.github.mundanej.mjo.orb.LocalInvocationUserException;
import io.github.mundanej.mjo.orb.LocalObjectReference;
import io.github.mundanej.mjo.orb.LocalOrb;
import io.github.mundanej.mjo.orb.OrbIdentity;
import io.github.mundanej.mjo.poa.Poa;
import io.github.mundanej.mjo.poa.PoaPolicySet;
import io.github.mundanej.mjo.typecode.IdlOperationDescriptor;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.omg.CORBA.BAD_INV_ORDER;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.OBJECT_NOT_EXIST;

/** Native Image smoke entry point for generated-style server dispatch. */
public final class GeneratedServerNativeSmoke {

  private GeneratedServerNativeSmoke() {}

  /** Runs a generated-style dispatcher through LocalOrb. */
  public static void main(String[] args) {
    LocalOrb orb = LocalOrb.create();
    LocalInvocationDispatcher dispatcher =
        request -> {
          SmokeAssertions.requireEquals(
              SmokeDescriptorFixtures.GREET, request.operation(), "operation descriptor");
          SmokeAssertions.requireEquals(List.of("Grace"), request.arguments(), "arguments");
          return "Hello Grace";
        };

    LocalObjectReference<SmokeDescriptorFixtures.Greeter> reference =
        orb.bindWithObjectId(
            SmokeDescriptorFixtures.Greeter.class,
            SmokeDescriptorFixtures.GREETER,
            "native-server",
            dispatcher);

    SmokeAssertions.requireEquals(
        "Hello Grace",
        orb.invoke(reference, SmokeDescriptorFixtures.GREET, List.of("Grace")),
        "server dispatch result");
    SmokeAssertions.requireEquals("native-server", reference.objectId(), "object id");

    LocalOrb durableOrb = LocalOrb.create(OrbIdentity.durable("native-registry-orb"));
    durableOrb.durablePoaPaths().register(List.of("RootPOA", "native"));
    durableOrb
        .durablePoaPaths()
        .requireRegistered(
            DurableObjectKey.fromPoaPath(
                "native-registry-orb", "/RootPOA/native", ascii("native-object"), 0));
    SmokeAssertions.requireThrows(
        BAD_PARAM.class,
        () -> durableOrb.durablePoaPaths().register(List.of("RootPOA", "native")),
        "duplicate durable POA path registration");
    SmokeAssertions.requireThrows(
        BAD_PARAM.class,
        () -> LocalOrb.create().durablePoaPaths().register(List.of("RootPOA")),
        "transient ORB durable POA path registration");
    durableOrb.shutdown();
    SmokeAssertions.requireThrows(
        BAD_INV_ORDER.class,
        () -> durableOrb.durablePoaPaths().contains(List.of("RootPOA", "native")),
        "durable POA path registry shutdown");

    LocalOrb activationOrb = LocalOrb.create(OrbIdentity.durable("native-activation-orb"));
    Poa root = Poa.createRoot(activationOrb);
    activationOrb.durablePoaPaths().register(List.of("RootPOA", "activated"));
    root.setAdapterActivator((parent, name) -> parent.createChild(name, persistentUserIdPolicy()));
    Poa activated =
        root.resolveDurablePoa(
            DurableObjectKey.fromPoaPath(
                "native-activation-orb", "/RootPOA/activated", ascii("native-object"), 0),
            true);
    SmokeAssertions.requireEquals("/RootPOA/activated", activated.path(), "durable POA lookup");
    SmokeAssertions.requireThrows(
        OBJECT_NOT_EXIST.class,
        () ->
            root.resolveDurablePoa(
                DurableObjectKey.fromPoaPath(
                    "native-activation-orb", "/RootPOA/missing", ascii("native-object"), 0),
                true),
        "unregistered durable POA lookup");

    LocalOrb rehydrationOrb = LocalOrb.create(OrbIdentity.durable("native-rehydrate-orb"));
    Poa rehydrationRoot = Poa.createRoot(rehydrationOrb, persistentUserIdServantManagerPolicy());
    rehydrationRoot.registerDurablePath();
    rehydrationRoot.setServantActivator((targetPoa, objectId) -> new NativeGreeter("Activated "));
    LocalObjectReference<SmokeDescriptorFixtures.Greeter> durableReference =
        rehydrationRoot.createReferenceWithId(
            "native-object",
            SmokeDescriptorFixtures.Greeter.class,
            SmokeDescriptorFixtures.GREETER,
            (servant, request) ->
                ((NativeGreeter) servant).greet((String) request.arguments().get(0)));
    LocalObjectReference<?> resolved =
        rehydrationRoot.resolveDurableReference(
            durableReference.durableObjectKey().orElseThrow(), true);
    SmokeAssertions.requireEquals(
        "Activated Grace",
        rehydrationOrb.invoke(resolved, SmokeDescriptorFixtures.GREET, List.of("Grace")),
        "durable POA servant-manager rehydration");

    IiopOrbServerHandler durableHandler =
        IiopOrbServerHandler.builder(rehydrationOrb)
            .bindDescriptor(
                SmokeDescriptorFixtures.GREETER,
                List.of(new IiopOperationBinding(SmokeDescriptorFixtures.GREET, new StringCodec())))
            .durableObjectResolver(key -> rehydrationRoot.resolveDurableReference(key, true))
            .build();
    GiopReply durableReply =
        durableHandler.handle(
            new GiopRequest(
                GiopHeader.forType(GiopMessageType.REQUEST),
                17,
                3,
                GiopTargetAddress.keyAddr(
                    durableReference.durableObjectKey().orElseThrow().encode()),
                "greet",
                List.of(),
                CdrWriter.bigEndian().writeString("Grace").toByteArray()));
    SmokeAssertions.requireEquals(
        GiopReplyStatus.NO_EXCEPTION, durableReply.replyStatus(), "durable IIOP reply status");
    SmokeAssertions.requireEquals(
        "Activated Grace",
        CdrReader.bigEndian(durableReply.body()).readString(),
        "durable IIOP resolver dispatch");
  }

  private static byte[] ascii(String value) {
    return value.getBytes(StandardCharsets.US_ASCII);
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

  private static PoaPolicySet persistentUserIdServantManagerPolicy() {
    return new PoaPolicySet(
        PoaPolicySet.ThreadPolicy.ORB_CTRL_MODEL,
        PoaPolicySet.LifespanPolicy.PERSISTENT,
        PoaPolicySet.IdUniquenessPolicy.UNIQUE_ID,
        PoaPolicySet.IdAssignmentPolicy.USER_ID,
        PoaPolicySet.ServantRetentionPolicy.RETAIN,
        PoaPolicySet.RequestProcessingPolicy.USE_SERVANT_MANAGER,
        PoaPolicySet.ImplicitActivationPolicy.NO_IMPLICIT_ACTIVATION);
  }

  private static final class NativeGreeter implements SmokeDescriptorFixtures.Greeter {

    private final String prefix;

    private NativeGreeter(String prefix) {
      this.prefix = prefix;
    }

    private String greet(String name) {
      return prefix + name;
    }
  }

  private static final class StringCodec implements IiopInvocationCodec {

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
    public byte[] encodeUserException(LocalInvocationUserException exception) {
      return CdrWriter.bigEndian()
          .writeString(exception.userException().getMessage())
          .toByteArray();
    }

    @Override
    public RuntimeException decodeUserException(
        IdlOperationDescriptor operation, String repositoryId, byte[] exceptionBody) {
      return new IllegalStateException(repositoryId);
    }
  }
}
