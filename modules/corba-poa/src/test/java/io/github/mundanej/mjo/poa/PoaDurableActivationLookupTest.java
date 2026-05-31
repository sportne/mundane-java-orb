package io.github.mundanej.mjo.poa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mundanej.mjo.orb.DurableObjectKey;
import io.github.mundanej.mjo.orb.LocalOrb;
import io.github.mundanej.mjo.orb.OrbIdentity;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.omg.CORBA.BAD_INV_ORDER;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.OBJECT_NOT_EXIST;

/** Tests for durable POA path lookup through registered adapter activation. */
@Tag("unit")
final class PoaDurableActivationLookupTest {

  @Test
  void durableLookupLocatesActiveRegisteredPersistentPoa() {
    LocalOrb orb = LocalOrb.create(OrbIdentity.durable("lookup-orb"));
    Poa root = Poa.createRoot(orb);
    Poa child = root.createChild("apps", persistentUserIdPolicy());
    child.registerDurablePath();
    DurableObjectKey key = durableKey("lookup-orb", "/RootPOA/apps", "alpha");

    assertSame(child, root.resolveDurablePoa(key, false));
  }

  @Test
  void durableLookupActivatesRegisteredMissingChildPath() {
    LocalOrb orb = LocalOrb.create(OrbIdentity.durable("lookup-orb"));
    Poa root = Poa.createRoot(orb);
    AtomicInteger activations = new AtomicInteger();
    orb.durablePoaPaths().register(List.of("RootPOA", "apps"));
    root.setAdapterActivator(
        (parent, name) -> {
          activations.incrementAndGet();
          return parent.createChild(name, persistentUserIdPolicy());
        });

    Poa child = root.resolveDurablePoa(durableKey("lookup-orb", "/RootPOA/apps", "alpha"), true);
    Poa again = root.resolveDurablePoa(durableKey("lookup-orb", "/RootPOA/apps", "beta"), true);

    assertEquals("/RootPOA/apps", child.path());
    assertSame(child, again);
    assertEquals(1, activations.get());
  }

  @Test
  void durableLookupRejectsUnregisteredAndWrongOrbPathsBeforeActivation() {
    LocalOrb orb = LocalOrb.create(OrbIdentity.durable("lookup-orb"));
    Poa root = Poa.createRoot(orb);
    AtomicInteger activations = new AtomicInteger();
    orb.durablePoaPaths().register(List.of("RootPOA", "apps"));
    root.setAdapterActivator(
        (parent, name) -> {
          activations.incrementAndGet();
          return parent.createChild(name, persistentUserIdPolicy());
        });

    assertObjectNotExist(
        () -> root.resolveDurablePoa(durableKey("lookup-orb", "/RootPOA/missing", "alpha"), true));
    assertObjectNotExist(
        () -> root.resolveDurablePoa(durableKey("other-orb", "/RootPOA/apps", "alpha"), true));
    assertBadParam(() -> root.resolveDurablePoa(null, true));
    assertEquals(0, activations.get());
  }

  @Test
  void durableLookupMapsAdapterActivatorFailuresDeterministically() {
    LocalOrb orb = LocalOrb.create(OrbIdentity.durable("lookup-orb"));
    Poa root = Poa.createRoot(orb);
    orb.durablePoaPaths().register(List.of("RootPOA", "apps"));
    root.setAdapterActivator(
        (parent, name) -> {
          throw new IllegalStateException("factory unavailable");
        });

    BAD_INV_ORDER failure =
        assertThrows(
            BAD_INV_ORDER.class,
            () -> root.resolveDurablePoa(durableKey("lookup-orb", "/RootPOA/apps", "alpha"), true));

    assertEquals(CompletionStatus.COMPLETED_NO, failure.completed);
  }

  @Test
  void durableLookupRejectsInactiveDestroyedAndTransientTargets() {
    LocalOrb firstOrb = LocalOrb.create(OrbIdentity.durable("lookup-orb"));
    Poa firstRoot = Poa.createRoot(firstOrb);
    Poa inactiveChild = firstRoot.createChild("apps", persistentUserIdPolicy());
    inactiveChild.registerDurablePath();
    inactiveChild.manager().deactivate();

    BAD_INV_ORDER inactive =
        assertThrows(
            BAD_INV_ORDER.class,
            () ->
                firstRoot.resolveDurablePoa(
                    durableKey("lookup-orb", "/RootPOA/apps", "alpha"), false));

    LocalOrb secondOrb = LocalOrb.create(OrbIdentity.durable("destroyed-orb"));
    Poa secondRoot = Poa.createRoot(secondOrb);
    Poa destroyedChild = secondRoot.createChild("apps", persistentUserIdPolicy());
    destroyedChild.registerDurablePath();
    destroyedChild.destroy();

    LocalOrb thirdOrb = LocalOrb.create(OrbIdentity.durable("transient-orb"));
    Poa transientRoot = Poa.createRoot(thirdOrb);
    thirdOrb.durablePoaPaths().register(List.of("RootPOA"));

    assertEquals(CompletionStatus.COMPLETED_NO, inactive.completed);
    assertObjectNotExist(
        () ->
            secondRoot.resolveDurablePoa(
                durableKey("destroyed-orb", "/RootPOA/apps", "alpha"), false));
    assertBadParam(
        () ->
            transientRoot.resolveDurablePoa(
                durableKey("transient-orb", "/RootPOA", "alpha"), false));
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

  private static DurableObjectKey durableKey(String orbId, String path, String objectId) {
    return DurableObjectKey.fromPoaPath(
        orbId, path, objectId.getBytes(StandardCharsets.US_ASCII), 0);
  }

  private static void assertBadParam(ThrowingAction action) {
    BAD_PARAM exception = assertThrows(BAD_PARAM.class, action::run);

    assertEquals(CompletionStatus.COMPLETED_NO, exception.completed);
  }

  private static void assertObjectNotExist(ThrowingAction action) {
    OBJECT_NOT_EXIST exception = assertThrows(OBJECT_NOT_EXIST.class, action::run);

    assertEquals(CompletionStatus.COMPLETED_NO, exception.completed);
  }

  @FunctionalInterface
  private interface ThrowingAction {
    void run();
  }
}
