package io.github.mundanej.mjo.poa;

import static io.github.mundanej.mjo.poa.PoaTestFixtures.GREETER_DESCRIPTOR;
import static io.github.mundanej.mjo.poa.PoaTestFixtures.GREETER_DISPATCHER;
import static io.github.mundanej.mjo.poa.PoaTestFixtures.invoke;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjo.orb.LocalObjectReference;
import io.github.mundanej.mjo.orb.LocalOrb;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.omg.CORBA.BAD_INV_ORDER;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.CORBA.UNKNOWN;

/** Negative and lifecycle coverage for the G6-630 local POA API. */
@Tag("unit")
final class PoaFailureModeTest {

  @Test
  void rootAccessorsExposeLocalIdentityAndPolicy() {
    Poa poa = Poa.createRoot(LocalOrb.create());

    assertEquals("RootPOA", poa.name());
    assertEquals("/RootPOA", poa.path());
    assertEquals(PoaPolicySet.poaLiteProfile(), poa.policySet());
    assertSame(poa.manager(), poa.manager());
    assertEquals(0, poa.activeObjectCount());
  }

  @Test
  void policySpecificSettersRejectWrongRequestProcessingModes() {
    Poa activeOnly = Poa.createRoot(LocalOrb.create());
    Poa retainedDefault =
        Poa.createRoot(
            LocalOrb.create(),
            policy(
                PoaPolicySet.IdAssignmentPolicy.SYSTEM_ID,
                PoaPolicySet.ServantRetentionPolicy.RETAIN,
                PoaPolicySet.RequestProcessingPolicy.USE_DEFAULT_SERVANT,
                PoaPolicySet.ImplicitActivationPolicy.NO_IMPLICIT_ACTIVATION));

    assertThrows(
        BAD_PARAM.class,
        () ->
            activeOnly.setDefaultServant(new PoaTestFixtures.GreeterServant(), GREETER_DISPATCHER));
    assertThrows(BAD_PARAM.class, () -> activeOnly.setServantActivator((poa, objectId) -> null));
    assertThrows(BAD_PARAM.class, () -> retainedDefault.setServantLocator(null));
  }

  @Test
  void idAssignmentSpecificMethodsRejectWrongPolicy() {
    Poa systemId = Poa.createRoot(LocalOrb.create());
    Poa userId =
        Poa.createRoot(
            LocalOrb.create(),
            policy(
                PoaPolicySet.IdAssignmentPolicy.USER_ID,
                PoaPolicySet.ServantRetentionPolicy.RETAIN,
                PoaPolicySet.RequestProcessingPolicy.USE_ACTIVE_OBJECT_MAP_ONLY,
                PoaPolicySet.ImplicitActivationPolicy.NO_IMPLICIT_ACTIVATION));

    assertThrows(
        BAD_PARAM.class,
        () ->
            systemId.activateServantWithId(
                "explicit",
                PoaTestFixtures.Greeter.class,
                GREETER_DESCRIPTOR,
                new PoaTestFixtures.GreeterServant(),
                GREETER_DISPATCHER));
    assertThrows(
        BAD_PARAM.class,
        () ->
            userId.activateServant(
                PoaTestFixtures.Greeter.class,
                GREETER_DESCRIPTOR,
                new PoaTestFixtures.GreeterServant(),
                GREETER_DISPATCHER));
  }

  @Test
  void duplicateChildAndWrongActivatorResultAreRejected() {
    Poa root = Poa.createRoot(LocalOrb.create());

    root.createChild("child", PoaPolicySet.transientRetainedProfile());

    assertThrows(
        BAD_PARAM.class, () -> root.createChild("child", PoaPolicySet.transientRetainedProfile()));

    Poa other =
        Poa.createRoot(LocalOrb.create())
            .createChild("other", PoaPolicySet.transientRetainedProfile());
    root.setAdapterActivator((parent, name) -> other);

    assertThrows(BAD_PARAM.class, () -> root.findChild("missing", true));
  }

  @Test
  void invalidChildNamesAndNullLifecycleCollaboratorsAreRejected() {
    Poa root = Poa.createRoot(LocalOrb.create());

    assertThrows(
        BAD_PARAM.class, () -> root.createChild(" ", PoaPolicySet.transientRetainedProfile()));
    assertThrows(BAD_PARAM.class, () -> root.findChild(null, false));
    assertThrows(BAD_PARAM.class, () -> root.setAdapterActivator(null));
  }

  @Test
  void duplicateUserObjectKeysAreRejectedForReferencesAndActivations() {
    LocalOrb orb = LocalOrb.create();
    Poa poa =
        Poa.createRoot(
            orb,
            policy(
                PoaPolicySet.IdAssignmentPolicy.USER_ID,
                PoaPolicySet.ServantRetentionPolicy.RETAIN,
                PoaPolicySet.RequestProcessingPolicy.USE_ACTIVE_OBJECT_MAP_ONLY,
                PoaPolicySet.ImplicitActivationPolicy.NO_IMPLICIT_ACTIVATION));

    poa.createReferenceWithId("object", PoaTestFixtures.Greeter.class, GREETER_DESCRIPTOR);

    BAD_PARAM duplicateReference =
        assertThrows(
            BAD_PARAM.class,
            () ->
                poa.createReferenceWithId(
                    "object", PoaTestFixtures.Greeter.class, GREETER_DESCRIPTOR));
    BAD_PARAM duplicateActivation =
        assertThrows(
            BAD_PARAM.class,
            () ->
                poa.activateServantWithId(
                    "object",
                    PoaTestFixtures.Greeter.class,
                    GREETER_DESCRIPTOR,
                    new PoaTestFixtures.GreeterServant(),
                    GREETER_DISPATCHER));

    assertEquals(org.omg.CORBA.CompletionStatus.COMPLETED_NO, duplicateReference.completed);
    assertEquals(org.omg.CORBA.CompletionStatus.COMPLETED_NO, duplicateActivation.completed);
  }

  @Test
  void missingServantResolutionPathsFailDeterministically() {
    LocalOrb orb = LocalOrb.create();
    Poa defaultPoa =
        Poa.createRoot(
            orb,
            policy(
                PoaPolicySet.IdAssignmentPolicy.SYSTEM_ID,
                PoaPolicySet.ServantRetentionPolicy.RETAIN,
                PoaPolicySet.RequestProcessingPolicy.USE_DEFAULT_SERVANT,
                PoaPolicySet.ImplicitActivationPolicy.NO_IMPLICIT_ACTIVATION));
    LocalObjectReference<PoaTestFixtures.Greeter> defaultReference =
        defaultPoa.createReference(PoaTestFixtures.Greeter.class, GREETER_DESCRIPTOR);

    assertThrows(OBJECT_NOT_EXIST.class, () -> invoke(orb, defaultReference));

    LocalOrb managerOrb = LocalOrb.create();
    Poa managerPoa =
        Poa.createRoot(
            managerOrb,
            policy(
                PoaPolicySet.IdAssignmentPolicy.SYSTEM_ID,
                PoaPolicySet.ServantRetentionPolicy.RETAIN,
                PoaPolicySet.RequestProcessingPolicy.USE_SERVANT_MANAGER,
                PoaPolicySet.ImplicitActivationPolicy.NO_IMPLICIT_ACTIVATION));
    LocalObjectReference<PoaTestFixtures.Greeter> managerReference =
        managerPoa.createReference(PoaTestFixtures.Greeter.class, GREETER_DESCRIPTOR);

    assertThrows(OBJECT_NOT_EXIST.class, () -> invoke(managerOrb, managerReference));
  }

  @Test
  void servantManagerReferenceWithoutDispatcherIsRejectedAfterActivatorLookup() {
    LocalOrb orb = LocalOrb.create();
    Poa poa =
        Poa.createRoot(
            orb,
            policy(
                PoaPolicySet.IdAssignmentPolicy.SYSTEM_ID,
                PoaPolicySet.ServantRetentionPolicy.RETAIN,
                PoaPolicySet.RequestProcessingPolicy.USE_SERVANT_MANAGER,
                PoaPolicySet.ImplicitActivationPolicy.NO_IMPLICIT_ACTIVATION));
    poa.setServantActivator((targetPoa, objectId) -> new PoaTestFixtures.GreeterServant());
    LocalObjectReference<PoaTestFixtures.Greeter> reference =
        poa.createReference(PoaTestFixtures.Greeter.class, GREETER_DESCRIPTOR);

    assertThrows(BAD_PARAM.class, () -> invoke(orb, reference));
  }

  @Test
  void defaultServantPrefersRetainedActiveObjectWhenPresent() {
    LocalOrb orb = LocalOrb.create();
    Poa poa =
        Poa.createRoot(
            orb,
            policy(
                PoaPolicySet.IdAssignmentPolicy.SYSTEM_ID,
                PoaPolicySet.ServantRetentionPolicy.RETAIN,
                PoaPolicySet.RequestProcessingPolicy.USE_DEFAULT_SERVANT,
                PoaPolicySet.ImplicitActivationPolicy.NO_IMPLICIT_ACTIVATION));
    poa.setDefaultServant(new PoaTestFixtures.GreeterServant("Default "), GREETER_DISPATCHER);
    LocalObjectReference<PoaTestFixtures.Greeter> reference =
        poa.activateServant(
            PoaTestFixtures.Greeter.class,
            GREETER_DESCRIPTOR,
            new PoaTestFixtures.GreeterServant("Active "),
            GREETER_DISPATCHER);

    assertEquals("Active Ada", invoke(orb, reference));
  }

  @Test
  void servantToReferenceReturnsExistingImplicitActivationAndRejectsDisabledImplicitActivation() {
    LocalOrb orb = LocalOrb.create();
    Poa implicitPoa =
        Poa.createRoot(
            orb,
            policy(
                PoaPolicySet.IdAssignmentPolicy.SYSTEM_ID,
                PoaPolicySet.ServantRetentionPolicy.RETAIN,
                PoaPolicySet.RequestProcessingPolicy.USE_ACTIVE_OBJECT_MAP_ONLY,
                PoaPolicySet.ImplicitActivationPolicy.IMPLICIT_ACTIVATION));
    PoaTestFixtures.GreeterServant servant = new PoaTestFixtures.GreeterServant();
    LocalObjectReference<PoaTestFixtures.Greeter> first =
        implicitPoa.servantToReference(
            PoaTestFixtures.Greeter.class, GREETER_DESCRIPTOR, servant, GREETER_DISPATCHER);
    LocalObjectReference<PoaTestFixtures.Greeter> second =
        implicitPoa.servantToReference(
            PoaTestFixtures.Greeter.class, GREETER_DESCRIPTOR, servant, GREETER_DISPATCHER);

    assertSame(first, second);

    Poa disabled = Poa.createRoot(LocalOrb.create());
    assertThrows(
        BAD_INV_ORDER.class,
        () ->
            disabled.servantToReference(
                PoaTestFixtures.Greeter.class,
                GREETER_DESCRIPTOR,
                new PoaTestFixtures.GreeterServant(),
                GREETER_DISPATCHER));
  }

  @Test
  void destroyIsIdempotentAndBlocksFurtherPoaOperations() {
    Poa poa = Poa.createRoot(LocalOrb.create());

    poa.destroy();
    poa.destroy();

    assertTrue(poa.isDestroyed());
    assertThrows(
        BAD_INV_ORDER.class,
        () -> poa.createChild("late", PoaPolicySet.transientRetainedProfile()));
  }

  @Test
  void destroyUnbindsRetainedReferencesFromLocalOrb() {
    LocalOrb orb = LocalOrb.create();
    Poa poa = Poa.createRoot(orb);
    LocalObjectReference<PoaTestFixtures.Greeter> reference =
        poa.activateServant(
            PoaTestFixtures.Greeter.class,
            GREETER_DESCRIPTOR,
            new PoaTestFixtures.GreeterServant(),
            GREETER_DISPATCHER);

    poa.destroy();

    assertThrows(OBJECT_NOT_EXIST.class, () -> invoke(orb, reference));
    assertEquals(0, poa.activeObjectCount());
  }

  @Test
  void nonRetainedDefaultServantDispatchesWithoutActiveObjectMapEntry() {
    LocalOrb orb = LocalOrb.create();
    Poa poa =
        Poa.createRoot(
            orb,
            policy(
                PoaPolicySet.IdAssignmentPolicy.SYSTEM_ID,
                PoaPolicySet.ServantRetentionPolicy.NON_RETAIN,
                PoaPolicySet.RequestProcessingPolicy.USE_DEFAULT_SERVANT,
                PoaPolicySet.ImplicitActivationPolicy.NO_IMPLICIT_ACTIVATION));
    poa.setDefaultServant(new PoaTestFixtures.GreeterServant("Default "), GREETER_DISPATCHER);
    LocalObjectReference<PoaTestFixtures.Greeter> reference =
        poa.createReference(PoaTestFixtures.Greeter.class, GREETER_DESCRIPTOR);

    assertEquals("Default Ada", invoke(orb, reference));
    assertEquals(0, poa.activeObjectCount());
  }

  @Test
  void locatorPostinvokeSeesDispatcherFailure() {
    LocalOrb orb = LocalOrb.create();
    Poa poa =
        Poa.createRoot(
            orb,
            policy(
                PoaPolicySet.IdAssignmentPolicy.SYSTEM_ID,
                PoaPolicySet.ServantRetentionPolicy.NON_RETAIN,
                PoaPolicySet.RequestProcessingPolicy.USE_SERVANT_MANAGER,
                PoaPolicySet.ImplicitActivationPolicy.NO_IMPLICIT_ACTIVATION));
    AtomicReference<Throwable> observedFailure = new AtomicReference<>();
    poa.setServantLocator(
        new PoaServantLocator() {
          @Override
          public PoaServantLocatorResult preinvoke(
              Poa targetPoa,
              String objectId,
              io.github.mundanej.mjo.modern.LocalInvocationRequest request) {
            return new PoaServantLocatorResult(new PoaTestFixtures.GreeterServant(), "cookie");
          }

          @Override
          public void postinvoke(
              Poa targetPoa,
              String objectId,
              io.github.mundanej.mjo.modern.LocalInvocationRequest request,
              PoaServantLocatorResult result,
              Object outcome,
              Throwable failure) {
            observedFailure.set(failure);
          }
        });
    LocalObjectReference<PoaTestFixtures.Greeter> reference =
        poa.createReference(
            PoaTestFixtures.Greeter.class,
            GREETER_DESCRIPTOR,
            (target, request) -> {
              throw new Exception("checked failure");
            });

    assertThrows(UNKNOWN.class, () -> invoke(orb, reference));
    assertEquals("checked failure", observedFailure.get().getMessage());
  }

  private static PoaPolicySet policy(
      PoaPolicySet.IdAssignmentPolicy idAssignmentPolicy,
      PoaPolicySet.ServantRetentionPolicy retentionPolicy,
      PoaPolicySet.RequestProcessingPolicy requestProcessingPolicy,
      PoaPolicySet.ImplicitActivationPolicy implicitActivationPolicy) {
    return new PoaPolicySet(
        PoaPolicySet.ThreadPolicy.ORB_CTRL_MODEL,
        PoaPolicySet.LifespanPolicy.TRANSIENT,
        PoaPolicySet.IdUniquenessPolicy.UNIQUE_ID,
        idAssignmentPolicy,
        retentionPolicy,
        requestProcessingPolicy,
        implicitActivationPolicy);
  }
}
