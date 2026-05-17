package io.github.mundanej.mjo.poa;

import static io.github.mundanej.mjo.poa.PoaTestFixtures.GREETER_DESCRIPTOR;
import static io.github.mundanej.mjo.poa.PoaTestFixtures.GREETER_DISPATCHER;
import static io.github.mundanej.mjo.poa.PoaTestFixtures.invoke;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjo.orb.LocalObjectReference;
import io.github.mundanej.mjo.orb.LocalOrb;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.omg.CORBA.BAD_INV_ORDER;
import org.omg.CORBA.CompletionStatus;

/** Tests for local POA manager request states. */
@Tag("unit")
final class PoaManagerStateTest {

  @Test
  void holdingRequestsBlocksUntilManagerBecomesActive() throws Exception {
    LocalOrb orb = LocalOrb.create();
    Poa poa = Poa.createRoot(orb);
    LocalObjectReference<PoaTestFixtures.Greeter> reference = activateGreeter(poa);
    poa.manager().holdRequests();
    ExecutorService executor = Executors.newSingleThreadExecutor();

    try {
      Future<String> held = executor.submit(() -> invoke(orb, reference));

      Thread.sleep(100L);
      assertFalse(held.isDone());

      poa.manager().activate();

      assertEquals("Hello Ada", held.get(1L, TimeUnit.SECONDS));
      assertEquals(PoaManager.State.ACTIVE, poa.manager().state());
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void discardingRequestsRejectsDispatchUntilReactivated() {
    LocalOrb orb = LocalOrb.create();
    Poa poa = Poa.createRoot(orb);
    LocalObjectReference<PoaTestFixtures.Greeter> reference = activateGreeter(poa);

    poa.manager().discardRequests();

    BAD_INV_ORDER failure = assertThrows(BAD_INV_ORDER.class, () -> invoke(orb, reference));

    assertEquals(CompletionStatus.COMPLETED_NO, failure.completed);

    poa.manager().activate();

    assertEquals("Hello Ada", invoke(orb, reference));
  }

  @Test
  void inactiveManagerRejectsDispatchAndNewActivation() {
    LocalOrb orb = LocalOrb.create();
    Poa poa = Poa.createRoot(orb);
    LocalObjectReference<PoaTestFixtures.Greeter> reference = activateGreeter(poa);

    poa.manager().deactivate();

    BAD_INV_ORDER invokeFailure = assertThrows(BAD_INV_ORDER.class, () -> invoke(orb, reference));
    BAD_INV_ORDER activationFailure =
        assertThrows(
            BAD_INV_ORDER.class,
            () ->
                poa.activateServant(
                    PoaTestFixtures.Greeter.class,
                    GREETER_DESCRIPTOR,
                    new PoaTestFixtures.GreeterServant(),
                    GREETER_DISPATCHER));

    assertEquals(CompletionStatus.COMPLETED_NO, invokeFailure.completed);
    assertEquals(CompletionStatus.COMPLETED_NO, activationFailure.completed);
  }

  @Test
  void inactiveManagerCannotBeReactivated() {
    PoaManager manager = new PoaManager();

    manager.deactivate();

    BAD_INV_ORDER activationFailure = assertThrows(BAD_INV_ORDER.class, manager::activate);

    assertEquals(PoaManager.State.INACTIVE, manager.state());
    assertEquals(CompletionStatus.COMPLETED_NO, activationFailure.completed);
  }

  @Test
  void singleThreadPolicySerializesDispatch() throws Exception {
    LocalOrb orb = LocalOrb.create();
    Poa poa =
        Poa.createRoot(
            orb,
            new PoaPolicySet(
                PoaPolicySet.ThreadPolicy.SINGLE_THREAD_MODEL,
                PoaPolicySet.LifespanPolicy.TRANSIENT,
                PoaPolicySet.IdUniquenessPolicy.UNIQUE_ID,
                PoaPolicySet.IdAssignmentPolicy.SYSTEM_ID,
                PoaPolicySet.ServantRetentionPolicy.RETAIN,
                PoaPolicySet.RequestProcessingPolicy.USE_ACTIVE_OBJECT_MAP_ONLY,
                PoaPolicySet.ImplicitActivationPolicy.NO_IMPLICIT_ACTIVATION));
    LocalObjectReference<PoaTestFixtures.Greeter> reference =
        poa.activateServant(
            PoaTestFixtures.Greeter.class,
            GREETER_DESCRIPTOR,
            new PoaTestFixtures.GreeterServant(),
            (target, request) -> {
              assertTrue(Thread.holdsLock(poa));
              return target.greet((String) request.arguments().get(0));
            });

    assertEquals("Hello Ada", invoke(orb, reference));
  }

  private static LocalObjectReference<PoaTestFixtures.Greeter> activateGreeter(Poa poa) {
    return poa.activateServant(
        PoaTestFixtures.Greeter.class,
        GREETER_DESCRIPTOR,
        new PoaTestFixtures.GreeterServant(),
        GREETER_DISPATCHER);
  }
}
