package io.github.mundanej.mjo.poa;

import static io.github.mundanej.mjo.poa.PoaTestFixtures.GREET;
import static io.github.mundanej.mjo.poa.PoaTestFixtures.GREETER_DESCRIPTOR;
import static io.github.mundanej.mjo.poa.PoaTestFixtures.GREETER_DISPATCHER;
import static io.github.mundanej.mjo.poa.PoaTestFixtures.invoke;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mundanej.mjo.orb.LocalObjectReference;
import io.github.mundanej.mjo.orb.LocalOrb;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.CompletionStatus;

/** Tests for the G6-630 POA policy matrix. */
@Tag("unit")
final class PoaPolicyCombinationTest {

  @Test
  void persistentPolicyIsDeterministicallyDeferred() {
    BAD_PARAM exception =
        assertThrows(
            BAD_PARAM.class,
            () ->
                policy(
                    PoaPolicySet.ThreadPolicy.ORB_CTRL_MODEL,
                    PoaPolicySet.LifespanPolicy.PERSISTENT,
                    PoaPolicySet.IdUniquenessPolicy.UNIQUE_ID,
                    PoaPolicySet.IdAssignmentPolicy.SYSTEM_ID,
                    PoaPolicySet.ServantRetentionPolicy.RETAIN,
                    PoaPolicySet.RequestProcessingPolicy.USE_ACTIVE_OBJECT_MAP_ONLY,
                    PoaPolicySet.ImplicitActivationPolicy.NO_IMPLICIT_ACTIVATION));

    assertEquals(CompletionStatus.COMPLETED_NO, exception.completed);
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

    assertEquals("local-1", reference.objectId());
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

    assertEquals("local-2", second.objectId());
    assertEquals("Hello Ada", invoke(orb, second));
    assertEquals(1, poa.activeObjectCount());
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
    BAD_PARAM exception = assertThrows(BAD_PARAM.class, action::run);

    assertEquals(CompletionStatus.COMPLETED_NO, exception.completed);
  }

  @FunctionalInterface
  private interface ThrowingAction {

    void run();
  }
}
