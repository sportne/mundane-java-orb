package io.github.mundanej.mjo.poa;

import static io.github.mundanej.mjo.poa.PoaTestFixtures.GREET;
import static io.github.mundanej.mjo.poa.PoaTestFixtures.GREETER_DESCRIPTOR;
import static io.github.mundanej.mjo.poa.PoaTestFixtures.GREETER_DISPATCHER;
import static io.github.mundanej.mjo.poa.PoaTestFixtures.invoke;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mundanej.mjo.orb.DurableObjectKey;
import io.github.mundanej.mjo.orb.LocalObjectReference;
import io.github.mundanej.mjo.orb.LocalOrb;
import io.github.mundanej.mjo.orb.OrbIdentity;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.OBJECT_NOT_EXIST;

/** Tests for the G6-630 POA policy matrix. */
@Tag("unit")
final class PoaPolicyCombinationTest {

  @Test
  void persistentPolicyRequiresDurableOrbAtPoaCreation() {
    PoaPolicySet persistent =
        policy(
            PoaPolicySet.ThreadPolicy.ORB_CTRL_MODEL,
            PoaPolicySet.LifespanPolicy.PERSISTENT,
            PoaPolicySet.IdUniquenessPolicy.UNIQUE_ID,
            PoaPolicySet.IdAssignmentPolicy.SYSTEM_ID,
            PoaPolicySet.ServantRetentionPolicy.RETAIN,
            PoaPolicySet.RequestProcessingPolicy.USE_ACTIVE_OBJECT_MAP_ONLY,
            PoaPolicySet.ImplicitActivationPolicy.NO_IMPLICIT_ACTIVATION);

    BAD_PARAM exception =
        assertThrows(BAD_PARAM.class, () -> Poa.createRoot(LocalOrb.create(), persistent));

    assertEquals(CompletionStatus.COMPLETED_NO, exception.completed);
    Poa durableRoot = Poa.createRoot(LocalOrb.create(OrbIdentity.durable("poa-orb")), persistent);
    assertEquals(PoaPolicySet.LifespanPolicy.PERSISTENT, durableRoot.policySet().lifespanPolicy());
  }

  @Test
  void invalidRetentionAndImplicitActivationCombinationsAreRejected() {
    assertRejected(
        () ->
            policy(
                PoaPolicySet.ThreadPolicy.ORB_CTRL_MODEL,
                PoaPolicySet.LifespanPolicy.TRANSIENT,
                PoaPolicySet.IdUniquenessPolicy.UNIQUE_ID,
                PoaPolicySet.IdAssignmentPolicy.SYSTEM_ID,
                PoaPolicySet.ServantRetentionPolicy.NON_RETAIN,
                PoaPolicySet.RequestProcessingPolicy.USE_ACTIVE_OBJECT_MAP_ONLY,
                PoaPolicySet.ImplicitActivationPolicy.NO_IMPLICIT_ACTIVATION));
    assertRejected(
        () ->
            policy(
                PoaPolicySet.ThreadPolicy.ORB_CTRL_MODEL,
                PoaPolicySet.LifespanPolicy.TRANSIENT,
                PoaPolicySet.IdUniquenessPolicy.UNIQUE_ID,
                PoaPolicySet.IdAssignmentPolicy.USER_ID,
                PoaPolicySet.ServantRetentionPolicy.RETAIN,
                PoaPolicySet.RequestProcessingPolicy.USE_DEFAULT_SERVANT,
                PoaPolicySet.ImplicitActivationPolicy.IMPLICIT_ACTIVATION));
  }

  @Test
  void userIdAndMultipleIdPoliciesAllowExplicitDuplicateServantActivation() {
    LocalOrb orb = LocalOrb.create();
    Poa poa =
        Poa.createRoot(
            orb,
            policy(
                PoaPolicySet.ThreadPolicy.ORB_CTRL_MODEL,
                PoaPolicySet.LifespanPolicy.TRANSIENT,
                PoaPolicySet.IdUniquenessPolicy.MULTIPLE_ID,
                PoaPolicySet.IdAssignmentPolicy.USER_ID,
                PoaPolicySet.ServantRetentionPolicy.RETAIN,
                PoaPolicySet.RequestProcessingPolicy.USE_ACTIVE_OBJECT_MAP_ONLY,
                PoaPolicySet.ImplicitActivationPolicy.NO_IMPLICIT_ACTIVATION));
    PoaTestFixtures.GreeterServant servant = new PoaTestFixtures.GreeterServant();

    LocalObjectReference<PoaTestFixtures.Greeter> first =
        poa.activateServantWithId(
            "alpha",
            PoaTestFixtures.Greeter.class,
            GREETER_DESCRIPTOR,
            servant,
            GREETER_DISPATCHER);
    LocalObjectReference<PoaTestFixtures.Greeter> second =
        poa.activateServantWithId(
            "beta", PoaTestFixtures.Greeter.class, GREETER_DESCRIPTOR, servant, GREETER_DISPATCHER);

    assertEquals("alpha", first.objectId());
    assertEquals("beta", second.objectId());
    assertEquals(2, poa.activeObjectCount());
    assertEquals("Hello Ada", orb.invoke(second, GREET, List.of("Ada")));
  }

  @Test
  void retainedDefaultServantHandlesMissingActiveObjectMapEntry() {
    LocalOrb orb = LocalOrb.create();
    Poa poa =
        Poa.createRoot(
            orb,
            policy(
                PoaPolicySet.ThreadPolicy.ORB_CTRL_MODEL,
                PoaPolicySet.LifespanPolicy.TRANSIENT,
                PoaPolicySet.IdUniquenessPolicy.UNIQUE_ID,
                PoaPolicySet.IdAssignmentPolicy.USER_ID,
                PoaPolicySet.ServantRetentionPolicy.RETAIN,
                PoaPolicySet.RequestProcessingPolicy.USE_DEFAULT_SERVANT,
                PoaPolicySet.ImplicitActivationPolicy.NO_IMPLICIT_ACTIVATION));
    poa.setDefaultServant(new PoaTestFixtures.GreeterServant("Default "), GREETER_DISPATCHER);
    LocalObjectReference<PoaTestFixtures.Greeter> reference =
        poa.createReferenceWithId("missing", PoaTestFixtures.Greeter.class, GREETER_DESCRIPTOR);

    assertEquals("Default Ada", invoke(orb, reference));
    assertEquals(0, poa.activeObjectCount());
  }

  @Test
  void implicitActivationCreatesSystemAssignedRetainedReference() {
    LocalOrb orb = LocalOrb.create();
    Poa poa =
        Poa.createRoot(
            orb,
            policy(
                PoaPolicySet.ThreadPolicy.ORB_CTRL_MODEL,
                PoaPolicySet.LifespanPolicy.TRANSIENT,
                PoaPolicySet.IdUniquenessPolicy.UNIQUE_ID,
                PoaPolicySet.IdAssignmentPolicy.SYSTEM_ID,
                PoaPolicySet.ServantRetentionPolicy.RETAIN,
                PoaPolicySet.RequestProcessingPolicy.USE_ACTIVE_OBJECT_MAP_ONLY,
                PoaPolicySet.ImplicitActivationPolicy.IMPLICIT_ACTIVATION));

    LocalObjectReference<PoaTestFixtures.Greeter> reference =
        poa.servantToReference(
            PoaTestFixtures.Greeter.class,
            GREETER_DESCRIPTOR,
            new PoaTestFixtures.GreeterServant(),
            GREETER_DISPATCHER);

    assertEquals("sys-1", reference.objectId());
    assertEquals("Hello Ada", invoke(orb, reference));
    assertEquals(1, poa.activeObjectCount());
  }

  @Test
  void deactivationReleasesUniqueIdServantForLaterActivation() {
    LocalOrb orb = LocalOrb.create();
    Poa poa = Poa.createRoot(orb);
    PoaTestFixtures.GreeterServant servant = new PoaTestFixtures.GreeterServant();
    LocalObjectReference<PoaTestFixtures.Greeter> first =
        poa.activateServant(
            PoaTestFixtures.Greeter.class, GREETER_DESCRIPTOR, servant, GREETER_DISPATCHER);

    poa.deactivateObject(first.objectId());
    LocalObjectReference<PoaTestFixtures.Greeter> second =
        poa.activateServant(
            PoaTestFixtures.Greeter.class, GREETER_DESCRIPTOR, servant, GREETER_DISPATCHER);

    assertEquals("sys-2", second.objectId());
    assertEquals("Hello Ada", invoke(orb, second));
    assertEquals(1, poa.activeObjectCount());
  }

  @Test
  void persistentUserIdReferencesCarryStableDurableKeysAcrossRestartSimulation() {
    PoaPolicySet persistentUserId =
        policy(
            PoaPolicySet.ThreadPolicy.ORB_CTRL_MODEL,
            PoaPolicySet.LifespanPolicy.PERSISTENT,
            PoaPolicySet.IdUniquenessPolicy.UNIQUE_ID,
            PoaPolicySet.IdAssignmentPolicy.USER_ID,
            PoaPolicySet.ServantRetentionPolicy.RETAIN,
            PoaPolicySet.RequestProcessingPolicy.USE_ACTIVE_OBJECT_MAP_ONLY,
            PoaPolicySet.ImplicitActivationPolicy.NO_IMPLICIT_ACTIVATION);
    LocalOrb firstOrb = LocalOrb.create(OrbIdentity.durable("restart-orb"));
    Poa firstPoa = Poa.createRoot(firstOrb, persistentUserId);
    LocalObjectReference<PoaTestFixtures.Greeter> first =
        firstPoa.activateServantWithId(
            "alpha",
            PoaTestFixtures.Greeter.class,
            GREETER_DESCRIPTOR,
            new PoaTestFixtures.GreeterServant(),
            GREETER_DISPATCHER);

    DurableObjectKey firstKey = first.durableObjectKey().orElseThrow();
    assertEquals("restart-orb", firstKey.orbId());
    assertEquals("/RootPOA", firstKey.poaPathString());
    assertArrayEquals("alpha".getBytes(StandardCharsets.US_ASCII), firstKey.objectId());
    assertEquals(first, firstPoa.referenceForDurableKey(firstKey));

    LocalOrb secondOrb = LocalOrb.create(OrbIdentity.durable("restart-orb"));
    Poa secondPoa = Poa.createRoot(secondOrb, persistentUserId);
    LocalObjectReference<PoaTestFixtures.Greeter> second =
        secondPoa.activateServantWithId(
            "alpha",
            PoaTestFixtures.Greeter.class,
            GREETER_DESCRIPTOR,
            new PoaTestFixtures.GreeterServant(),
            GREETER_DISPATCHER);

    assertEquals(firstKey, second.durableObjectKey().orElseThrow());
    assertEquals(second, secondPoa.referenceForDurableKey(firstKey));
  }

  @Test
  void persistentSystemIdReferencesUseDeterministicPerPoaIds() {
    PoaPolicySet persistentSystemId =
        policy(
            PoaPolicySet.ThreadPolicy.ORB_CTRL_MODEL,
            PoaPolicySet.LifespanPolicy.PERSISTENT,
            PoaPolicySet.IdUniquenessPolicy.UNIQUE_ID,
            PoaPolicySet.IdAssignmentPolicy.SYSTEM_ID,
            PoaPolicySet.ServantRetentionPolicy.RETAIN,
            PoaPolicySet.RequestProcessingPolicy.USE_ACTIVE_OBJECT_MAP_ONLY,
            PoaPolicySet.ImplicitActivationPolicy.NO_IMPLICIT_ACTIVATION);
    Poa firstPoa =
        Poa.createRoot(LocalOrb.create(OrbIdentity.durable("restart-orb")), persistentSystemId);
    LocalObjectReference<PoaTestFixtures.Greeter> first =
        firstPoa.activateServant(
            PoaTestFixtures.Greeter.class,
            GREETER_DESCRIPTOR,
            new PoaTestFixtures.GreeterServant(),
            GREETER_DISPATCHER);
    Poa secondPoa =
        Poa.createRoot(LocalOrb.create(OrbIdentity.durable("restart-orb")), persistentSystemId);
    LocalObjectReference<PoaTestFixtures.Greeter> second =
        secondPoa.activateServant(
            PoaTestFixtures.Greeter.class,
            GREETER_DESCRIPTOR,
            new PoaTestFixtures.GreeterServant(),
            GREETER_DISPATCHER);

    assertEquals("sys-1", first.objectId());
    assertEquals("sys-1", second.objectId());
    assertEquals(first.durableObjectKey().orElseThrow(), second.durableObjectKey().orElseThrow());
  }

  @Test
  void persistentChildPoaObjectIdsAreNamespacedByDurablePath() {
    LocalOrb orb = LocalOrb.create(OrbIdentity.durable("multi-poa-orb"));
    Poa root = Poa.createRoot(orb);
    PoaPolicySet persistentUserId =
        policy(
            PoaPolicySet.ThreadPolicy.ORB_CTRL_MODEL,
            PoaPolicySet.LifespanPolicy.PERSISTENT,
            PoaPolicySet.IdUniquenessPolicy.UNIQUE_ID,
            PoaPolicySet.IdAssignmentPolicy.USER_ID,
            PoaPolicySet.ServantRetentionPolicy.RETAIN,
            PoaPolicySet.RequestProcessingPolicy.USE_ACTIVE_OBJECT_MAP_ONLY,
            PoaPolicySet.ImplicitActivationPolicy.NO_IMPLICIT_ACTIVATION);
    Poa left = root.createChild("left", persistentUserId);
    Poa right = root.createChild("right", persistentUserId);

    LocalObjectReference<PoaTestFixtures.Greeter> leftReference =
        left.activateServantWithId(
            "alpha",
            PoaTestFixtures.Greeter.class,
            GREETER_DESCRIPTOR,
            new PoaTestFixtures.GreeterServant("Left "),
            GREETER_DISPATCHER);
    LocalObjectReference<PoaTestFixtures.Greeter> rightReference =
        right.activateServantWithId(
            "alpha",
            PoaTestFixtures.Greeter.class,
            GREETER_DESCRIPTOR,
            new PoaTestFixtures.GreeterServant("Right "),
            GREETER_DISPATCHER);

    assertEquals("alpha", leftReference.objectId());
    assertEquals("alpha", rightReference.objectId());
    assertEquals("Left Ada", invoke(orb, leftReference));
    assertEquals("Right Ada", invoke(orb, rightReference));
    assertEquals("/RootPOA/left", leftReference.durableObjectKey().orElseThrow().poaPathString());
    assertEquals("/RootPOA/right", rightReference.durableObjectKey().orElseThrow().poaPathString());
  }

  @Test
  void persistentChildPoaRejectsUnrepresentableDurablePathComponents() {
    LocalOrb orb = LocalOrb.create(OrbIdentity.durable("path-orb"));
    Poa root = Poa.createRoot(orb);
    PoaPolicySet persistentSystemId =
        policy(
            PoaPolicySet.ThreadPolicy.ORB_CTRL_MODEL,
            PoaPolicySet.LifespanPolicy.PERSISTENT,
            PoaPolicySet.IdUniquenessPolicy.UNIQUE_ID,
            PoaPolicySet.IdAssignmentPolicy.SYSTEM_ID,
            PoaPolicySet.ServantRetentionPolicy.RETAIN,
            PoaPolicySet.RequestProcessingPolicy.USE_ACTIVE_OBJECT_MAP_ONLY,
            PoaPolicySet.ImplicitActivationPolicy.NO_IMPLICIT_ACTIVATION);

    assertBadParam(() -> root.createChild("a/b", persistentSystemId));
    assertBadParam(() -> root.createChild("..", persistentSystemId));

    Poa transientChild = root.createChild("a/b", PoaPolicySet.transientRetainedProfile());
    assertBadParam(() -> transientChild.createChild("nested", persistentSystemId));
  }

  @Test
  void persistentDurableKeyLookupRejectsWrongStaleAndMalformedKeys() {
    PoaPolicySet persistentUserId =
        policy(
            PoaPolicySet.ThreadPolicy.ORB_CTRL_MODEL,
            PoaPolicySet.LifespanPolicy.PERSISTENT,
            PoaPolicySet.IdUniquenessPolicy.UNIQUE_ID,
            PoaPolicySet.IdAssignmentPolicy.USER_ID,
            PoaPolicySet.ServantRetentionPolicy.RETAIN,
            PoaPolicySet.RequestProcessingPolicy.USE_ACTIVE_OBJECT_MAP_ONLY,
            PoaPolicySet.ImplicitActivationPolicy.NO_IMPLICIT_ACTIVATION);
    Poa poa = Poa.createRoot(LocalOrb.create(OrbIdentity.durable("lookup-orb")), persistentUserId);
    LocalObjectReference<PoaTestFixtures.Greeter> reference =
        poa.activateServantWithId(
            "alpha",
            PoaTestFixtures.Greeter.class,
            GREETER_DESCRIPTOR,
            new PoaTestFixtures.GreeterServant(),
            GREETER_DISPATCHER);

    assertObjectNotExist(
        () ->
            poa.referenceForDurableKey(
                DurableObjectKey.fromPoaPath("other-orb", "/RootPOA", ascii("alpha"), 0)));
    assertObjectNotExist(
        () ->
            poa.referenceForDurableKey(
                DurableObjectKey.fromPoaPath("lookup-orb", "/RootPOA/child", ascii("alpha"), 0)));
    assertObjectNotExist(
        () ->
            poa.referenceForDurableKey(
                DurableObjectKey.fromPoaPath("lookup-orb", "/RootPOA", ascii("missing"), 0)));
    assertBadParam(
        () ->
            poa.referenceForDurableKey(
                new DurableObjectKey(
                    "lookup-orb", List.of("RootPOA"), new byte[] {(byte) 0xFF}, 0)));
    assertBadParam(
        () ->
            poa.referenceForDurableKey(
                new DurableObjectKey(
                    "lookup-orb",
                    List.of("RootPOA"),
                    reference.objectId().getBytes(StandardCharsets.US_ASCII),
                    1)));
  }

  @Test
  void persistentDurableKeysRejectHostileObjectIdBounds() {
    Poa poa =
        Poa.createRoot(
            LocalOrb.create(OrbIdentity.durable("bounds-orb")),
            policy(
                PoaPolicySet.ThreadPolicy.ORB_CTRL_MODEL,
                PoaPolicySet.LifespanPolicy.PERSISTENT,
                PoaPolicySet.IdUniquenessPolicy.UNIQUE_ID,
                PoaPolicySet.IdAssignmentPolicy.USER_ID,
                PoaPolicySet.ServantRetentionPolicy.RETAIN,
                PoaPolicySet.RequestProcessingPolicy.USE_ACTIVE_OBJECT_MAP_ONLY,
                PoaPolicySet.ImplicitActivationPolicy.NO_IMPLICIT_ACTIVATION));

    assertBadParam(
        () ->
            poa.activateServantWithId(
                "x".repeat(4097),
                PoaTestFixtures.Greeter.class,
                GREETER_DESCRIPTOR,
                new PoaTestFixtures.GreeterServant(),
                GREETER_DISPATCHER));
    assertBadParam(
        () ->
            poa.activateServantWithId(
                "snowman-\u2603",
                PoaTestFixtures.Greeter.class,
                GREETER_DESCRIPTOR,
                new PoaTestFixtures.GreeterServant(),
                GREETER_DISPATCHER));
  }

  private static PoaPolicySet policy(
      PoaPolicySet.ThreadPolicy threadPolicy,
      PoaPolicySet.LifespanPolicy lifespanPolicy,
      PoaPolicySet.IdUniquenessPolicy idUniquenessPolicy,
      PoaPolicySet.IdAssignmentPolicy idAssignmentPolicy,
      PoaPolicySet.ServantRetentionPolicy servantRetentionPolicy,
      PoaPolicySet.RequestProcessingPolicy requestProcessingPolicy,
      PoaPolicySet.ImplicitActivationPolicy implicitActivationPolicy) {
    return new PoaPolicySet(
        threadPolicy,
        lifespanPolicy,
        idUniquenessPolicy,
        idAssignmentPolicy,
        servantRetentionPolicy,
        requestProcessingPolicy,
        implicitActivationPolicy);
  }

  private static void assertRejected(ThrowingAction action) {
    assertBadParam(action);
  }

  private static void assertBadParam(ThrowingAction action) {
    BAD_PARAM exception = assertThrows(BAD_PARAM.class, action::run);

    assertEquals(CompletionStatus.COMPLETED_NO, exception.completed);
  }

  private static void assertObjectNotExist(ThrowingAction action) {
    OBJECT_NOT_EXIST exception = assertThrows(OBJECT_NOT_EXIST.class, action::run);

    assertEquals(CompletionStatus.COMPLETED_NO, exception.completed);
  }

  private static byte[] ascii(String value) {
    return value.getBytes(StandardCharsets.US_ASCII);
  }

  @FunctionalInterface
  private interface ThrowingAction {

    void run();
  }
}
