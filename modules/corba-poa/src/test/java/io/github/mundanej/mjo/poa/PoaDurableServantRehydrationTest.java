package io.github.mundanej.mjo.poa;

import static io.github.mundanej.mjo.poa.PoaTestFixtures.GREETER_DESCRIPTOR;
import static io.github.mundanej.mjo.poa.PoaTestFixtures.GREETER_DISPATCHER;
import static io.github.mundanej.mjo.poa.PoaTestFixtures.OBJECT_GREETER_DISPATCHER;
import static io.github.mundanej.mjo.poa.PoaTestFixtures.invoke;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mundanej.mjo.orb.DurableObjectKey;
import io.github.mundanej.mjo.orb.LocalObjectReference;
import io.github.mundanej.mjo.orb.LocalOrb;
import io.github.mundanej.mjo.orb.OrbIdentity;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.CORBA.UNKNOWN;

/** Durable object-key lookup coverage for POA servant-manager request paths. */
@Tag("unit")
final class PoaDurableServantRehydrationTest {

  @Test
  void retainedServantActivatorRehydratesDurableReferenceOnce() {
    LocalOrb orb = LocalOrb.create(OrbIdentity.durable("rehydrate-activator-orb"));
    Poa root =
        Poa.createRoot(
            orb, persistentUserId(PoaPolicySet.RequestProcessingPolicy.USE_SERVANT_MANAGER));
    root.registerDurablePath();
    AtomicInteger incarnations = new AtomicInteger();
    root.setServantActivator(
        (targetPoa, objectId) -> {
          assertSame(root, targetPoa);
          assertEquals("alpha", objectId);
          incarnations.incrementAndGet();
          return new PoaTestFixtures.GreeterServant("Activated ");
        });
    LocalObjectReference<PoaTestFixtures.Greeter> template =
        root.createReferenceWithId(
            "alpha", PoaTestFixtures.Greeter.class, GREETER_DESCRIPTOR, OBJECT_GREETER_DISPATCHER);

    LocalObjectReference<?> resolved =
        root.resolveDurableReference(template.durableObjectKey().orElseThrow(), true);

    assertEquals(template, resolved);
    assertEquals("Activated Ada", invoke(orb, resolved));
    assertEquals("Activated Ada", invoke(orb, resolved));
    assertEquals(1, incarnations.get());
    assertEquals(1, root.activeObjectCount());
  }

  @Test
  void activeObjectMapReferenceCanBeResolvedButMissingIdsDoNotRehydrate() {
    LocalOrb orb = LocalOrb.create(OrbIdentity.durable("rehydrate-active-orb"));
    Poa root =
        Poa.createRoot(
            orb, persistentUserId(PoaPolicySet.RequestProcessingPolicy.USE_ACTIVE_OBJECT_MAP_ONLY));
    root.registerDurablePath();
    LocalObjectReference<PoaTestFixtures.Greeter> active =
        root.activateServantWithId(
            "active",
            PoaTestFixtures.Greeter.class,
            GREETER_DESCRIPTOR,
            new PoaTestFixtures.GreeterServant("Active "),
            GREETER_DISPATCHER);

    assertEquals(
        active, root.resolveDurableReference(active.durableObjectKey().orElseThrow(), true));
    assertEquals("Active Ada", invoke(orb, active));
    assertThrows(
        OBJECT_NOT_EXIST.class,
        () ->
            root.resolveDurableReference(
                durableKey("rehydrate-active-orb", "/RootPOA", "missing"), true));
  }

  @Test
  void defaultServantReceivesResolvedDurableReferenceWithoutRetainedEntry() {
    LocalOrb orb = LocalOrb.create(OrbIdentity.durable("rehydrate-default-orb"));
    Poa root =
        Poa.createRoot(
            orb, persistentUserId(PoaPolicySet.RequestProcessingPolicy.USE_DEFAULT_SERVANT));
    root.registerDurablePath();
    root.setDefaultServant(new PoaTestFixtures.GreeterServant("Default "), GREETER_DISPATCHER);
    LocalObjectReference<PoaTestFixtures.Greeter> template =
        root.createReferenceWithId("defaulted", PoaTestFixtures.Greeter.class, GREETER_DESCRIPTOR);

    LocalObjectReference<?> resolved =
        root.resolveDurableReference(template.durableObjectKey().orElseThrow(), true);

    assertEquals(template, resolved);
    assertEquals("Default Ada", invoke(orb, resolved));
    assertEquals(0, root.activeObjectCount());
  }

  @Test
  void nonRetainedServantLocatorRemainsExplicitAndDoesNotPersistServants() {
    LocalOrb orb = LocalOrb.create(OrbIdentity.durable("rehydrate-locator-orb"));
    Poa root = Poa.createRoot(orb, persistentNonRetainServantManager());
    root.registerDurablePath();
    AtomicInteger preinvoke = new AtomicInteger();
    AtomicInteger postinvoke = new AtomicInteger();
    root.setServantLocator(
        new PoaServantLocator() {
          @Override
          public PoaServantLocatorResult preinvoke(
              Poa poa,
              String objectId,
              io.github.mundanej.mjo.modern.LocalInvocationRequest request) {
            assertSame(root, poa);
            assertEquals("located", objectId);
            preinvoke.incrementAndGet();
            return new PoaServantLocatorResult(
                new PoaTestFixtures.GreeterServant("Located "), "cookie");
          }

          @Override
          public void postinvoke(
              Poa poa,
              String objectId,
              io.github.mundanej.mjo.modern.LocalInvocationRequest request,
              PoaServantLocatorResult result,
              Object outcome,
              Throwable failure) {
            assertSame(root, poa);
            assertEquals("Located Ada", outcome);
            assertEquals(null, failure);
            postinvoke.incrementAndGet();
          }
        });
    LocalObjectReference<PoaTestFixtures.Greeter> template =
        root.createReferenceWithId(
            "located",
            PoaTestFixtures.Greeter.class,
            GREETER_DESCRIPTOR,
            OBJECT_GREETER_DISPATCHER);

    LocalObjectReference<?> resolved =
        root.resolveDurableReference(template.durableObjectKey().orElseThrow(), true);

    assertEquals("Located Ada", invoke(orb, resolved));
    assertEquals("Located Ada", invoke(orb, resolved));
    assertEquals(2, preinvoke.get());
    assertEquals(2, postinvoke.get());
    assertEquals(0, root.activeObjectCount());
  }

  @Test
  void missingDispatcherAndServantManagerFailuresMapDeterministically() {
    LocalOrb missingDispatcherOrb =
        LocalOrb.create(OrbIdentity.durable("rehydrate-missing-dispatcher"));
    Poa missingDispatcher =
        Poa.createRoot(
            missingDispatcherOrb,
            persistentUserId(PoaPolicySet.RequestProcessingPolicy.USE_SERVANT_MANAGER));
    missingDispatcher.registerDurablePath();
    missingDispatcher.setServantActivator(
        (targetPoa, objectId) -> new PoaTestFixtures.GreeterServant());
    LocalObjectReference<PoaTestFixtures.Greeter> missingDispatcherTemplate =
        missingDispatcher.createReferenceWithId(
            "missing-dispatcher", PoaTestFixtures.Greeter.class, GREETER_DESCRIPTOR);
    LocalObjectReference<?> missingDispatcherReference =
        missingDispatcher.resolveDurableReference(
            missingDispatcherTemplate.durableObjectKey().orElseThrow(), true);

    assertThrows(BAD_PARAM.class, () -> invoke(missingDispatcherOrb, missingDispatcherReference));

    LocalOrb failingOrb = LocalOrb.create(OrbIdentity.durable("rehydrate-failing-activator"));
    Poa failing =
        Poa.createRoot(
            failingOrb, persistentUserId(PoaPolicySet.RequestProcessingPolicy.USE_SERVANT_MANAGER));
    failing.registerDurablePath();
    failing.setServantActivator(
        (targetPoa, objectId) -> {
          throw new Exception("activation failed");
        });
    LocalObjectReference<PoaTestFixtures.Greeter> failingTemplate =
        failing.createReferenceWithId(
            "failure",
            PoaTestFixtures.Greeter.class,
            GREETER_DESCRIPTOR,
            OBJECT_GREETER_DISPATCHER);
    LocalObjectReference<?> failingReference =
        failing.resolveDurableReference(failingTemplate.durableObjectKey().orElseThrow(), true);

    assertThrows(UNKNOWN.class, () -> invoke(failingOrb, failingReference));
  }

  @Test
  void staleAndHostileDurableObjectIdsAreRejectedBeforeDispatch() {
    LocalOrb orb = LocalOrb.create(OrbIdentity.durable("rehydrate-hostile-orb"));
    Poa root =
        Poa.createRoot(
            orb, persistentUserId(PoaPolicySet.RequestProcessingPolicy.USE_DEFAULT_SERVANT));
    root.registerDurablePath();
    root.setDefaultServant(new PoaTestFixtures.GreeterServant("Default "), GREETER_DISPATCHER);

    assertThrows(
        OBJECT_NOT_EXIST.class,
        () ->
            root.resolveDurableReference(
                durableKey("rehydrate-hostile-orb", "/RootPOA", "stale"), true));
    assertThrows(
        BAD_PARAM.class,
        () ->
            root.resolveDurableReference(
                new DurableObjectKey(
                    "rehydrate-hostile-orb", List.of("RootPOA"), new byte[] {(byte) 0xff}, 0),
                true));
  }

  private static DurableObjectKey durableKey(String orbId, String path, String objectId) {
    return DurableObjectKey.fromPoaPath(
        orbId, path, objectId.getBytes(StandardCharsets.US_ASCII), 0);
  }

  private static PoaPolicySet persistentUserId(
      PoaPolicySet.RequestProcessingPolicy requestProcessingPolicy) {
    return new PoaPolicySet(
        PoaPolicySet.ThreadPolicy.ORB_CTRL_MODEL,
        PoaPolicySet.LifespanPolicy.PERSISTENT,
        PoaPolicySet.IdUniquenessPolicy.UNIQUE_ID,
        PoaPolicySet.IdAssignmentPolicy.USER_ID,
        PoaPolicySet.ServantRetentionPolicy.RETAIN,
        requestProcessingPolicy,
        PoaPolicySet.ImplicitActivationPolicy.NO_IMPLICIT_ACTIVATION);
  }

  private static PoaPolicySet persistentNonRetainServantManager() {
    return new PoaPolicySet(
        PoaPolicySet.ThreadPolicy.ORB_CTRL_MODEL,
        PoaPolicySet.LifespanPolicy.PERSISTENT,
        PoaPolicySet.IdUniquenessPolicy.UNIQUE_ID,
        PoaPolicySet.IdAssignmentPolicy.USER_ID,
        PoaPolicySet.ServantRetentionPolicy.NON_RETAIN,
        PoaPolicySet.RequestProcessingPolicy.USE_SERVANT_MANAGER,
        PoaPolicySet.ImplicitActivationPolicy.NO_IMPLICIT_ACTIVATION);
  }
}
