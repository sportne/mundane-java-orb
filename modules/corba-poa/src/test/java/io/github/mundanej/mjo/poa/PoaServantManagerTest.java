package io.github.mundanej.mjo.poa;

import static io.github.mundanej.mjo.poa.PoaTestFixtures.GREETER_DESCRIPTOR;
import static io.github.mundanej.mjo.poa.PoaTestFixtures.OBJECT_GREETER_DISPATCHER;
import static io.github.mundanej.mjo.poa.PoaTestFixtures.invoke;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.mundanej.mjo.orb.LocalObjectReference;
import io.github.mundanej.mjo.orb.LocalOrb;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for retained activators and non-retained servant locators. */
@Tag("unit")
final class PoaServantManagerTest {

  @Test
  void retainedServantActivatorIncarnatesMissingObjectOnce() {
    LocalOrb orb = LocalOrb.create();
    Poa poa =
        Poa.createRoot(
            orb,
            policy(
                PoaPolicySet.ServantRetentionPolicy.RETAIN,
                PoaPolicySet.RequestProcessingPolicy.USE_SERVANT_MANAGER));
    AtomicInteger incarnations = new AtomicInteger();
    poa.setServantActivator(
        (targetPoa, objectId) -> {
          assertEquals(poa, targetPoa);
          assertEquals("sys-1", objectId);
          incarnations.incrementAndGet();
          return new PoaTestFixtures.GreeterServant("Activated ");
        });
    LocalObjectReference<PoaTestFixtures.Greeter> reference =
        poa.createReference(
            PoaTestFixtures.Greeter.class, GREETER_DESCRIPTOR, OBJECT_GREETER_DISPATCHER);

    assertEquals("Activated Ada", invoke(orb, reference));
    assertEquals("Activated Ada", invoke(orb, reference));
    assertEquals(1, incarnations.get());
    assertEquals(1, poa.activeObjectCount());
  }

  @Test
  void nonRetainedServantLocatorRunsPreinvokeAndPostinvokeForEachRequest() {
    LocalOrb orb = LocalOrb.create();
    Poa poa =
        Poa.createRoot(
            orb,
            policy(
                PoaPolicySet.ServantRetentionPolicy.NON_RETAIN,
                PoaPolicySet.RequestProcessingPolicy.USE_SERVANT_MANAGER));
    AtomicInteger preinvoke = new AtomicInteger();
    AtomicInteger postinvoke = new AtomicInteger();
    AtomicReference<Object> observedCookie = new AtomicReference<>();
    poa.setServantLocator(
        new PoaServantLocator() {
          @Override
          public PoaServantLocatorResult preinvoke(
              Poa targetPoa,
              String objectId,
              io.github.mundanej.mjo.modern.LocalInvocationRequest request) {
            assertEquals(poa, targetPoa);
            assertEquals("sys-1", objectId);
            preinvoke.incrementAndGet();
            return new PoaServantLocatorResult(new PoaTestFixtures.GreeterServant("Located "), "c");
          }

          @Override
          public void postinvoke(
              Poa targetPoa,
              String objectId,
              io.github.mundanej.mjo.modern.LocalInvocationRequest request,
              PoaServantLocatorResult result,
              Object outcome,
              Throwable failure) {
            assertEquals(poa, targetPoa);
            assertEquals("Located Ada", outcome);
            assertEquals(null, failure);
            observedCookie.set(result.cookie());
            postinvoke.incrementAndGet();
          }
        });
    LocalObjectReference<PoaTestFixtures.Greeter> reference =
        poa.createReference(
            PoaTestFixtures.Greeter.class, GREETER_DESCRIPTOR, OBJECT_GREETER_DISPATCHER);

    assertEquals("Located Ada", invoke(orb, reference));
    assertEquals("Located Ada", invoke(orb, reference));
    assertEquals(2, preinvoke.get());
    assertEquals(2, postinvoke.get());
    assertEquals("c", observedCookie.get());
    assertEquals(0, poa.activeObjectCount());
  }

  private static PoaPolicySet policy(
      PoaPolicySet.ServantRetentionPolicy retentionPolicy,
      PoaPolicySet.RequestProcessingPolicy requestProcessingPolicy) {
    return new PoaPolicySet(
        PoaPolicySet.ThreadPolicy.ORB_CTRL_MODEL,
        PoaPolicySet.LifespanPolicy.TRANSIENT,
        PoaPolicySet.IdUniquenessPolicy.UNIQUE_ID,
        PoaPolicySet.IdAssignmentPolicy.SYSTEM_ID,
        retentionPolicy,
        requestProcessingPolicy,
        PoaPolicySet.ImplicitActivationPolicy.NO_IMPLICIT_ACTIVATION);
  }
}
