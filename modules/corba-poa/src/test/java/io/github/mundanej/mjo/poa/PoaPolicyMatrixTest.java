package io.github.mundanej.mjo.poa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.CompletionStatus;

/** Tests for the approved POA-lite policy profile. */
@Tag("unit")
final class PoaPolicyMatrixTest {

  @Test
  void approvedProfileIsAccepted() {
    PoaLitePolicySet policySet = PoaLitePolicySet.approvedProfile();

    assertEquals(PoaLitePolicySet.ThreadPolicy.ORB_CTRL_MODEL, policySet.threadPolicy());
    assertEquals(PoaLitePolicySet.LifespanPolicy.TRANSIENT, policySet.lifespanPolicy());
    assertEquals(PoaLitePolicySet.IdUniquenessPolicy.UNIQUE_ID, policySet.idUniquenessPolicy());
    assertEquals(PoaLitePolicySet.IdAssignmentPolicy.SYSTEM_ID, policySet.idAssignmentPolicy());
    assertEquals(
        PoaLitePolicySet.ServantRetentionPolicy.RETAIN, policySet.servantRetentionPolicy());
    assertEquals(
        PoaLitePolicySet.RequestProcessingPolicy.USE_ACTIVE_OBJECT_MAP_ONLY,
        policySet.requestProcessingPolicy());
    assertEquals(
        PoaLitePolicySet.ImplicitActivationPolicy.NO_IMPLICIT_ACTIVATION,
        policySet.implicitActivationPolicy());
  }

  @Test
  void unsupportedPolicyValuesAreRejected() {
    assertRejected(
        newPolicy(
            PoaLitePolicySet.ThreadPolicy.SINGLE_THREAD_MODEL,
            PoaLitePolicySet.LifespanPolicy.TRANSIENT,
            PoaLitePolicySet.IdUniquenessPolicy.UNIQUE_ID,
            PoaLitePolicySet.IdAssignmentPolicy.SYSTEM_ID,
            PoaLitePolicySet.ServantRetentionPolicy.RETAIN,
            PoaLitePolicySet.RequestProcessingPolicy.USE_ACTIVE_OBJECT_MAP_ONLY,
            PoaLitePolicySet.ImplicitActivationPolicy.NO_IMPLICIT_ACTIVATION));
    assertRejected(
        newPolicy(
            PoaLitePolicySet.ThreadPolicy.ORB_CTRL_MODEL,
            PoaLitePolicySet.LifespanPolicy.PERSISTENT,
            PoaLitePolicySet.IdUniquenessPolicy.UNIQUE_ID,
            PoaLitePolicySet.IdAssignmentPolicy.SYSTEM_ID,
            PoaLitePolicySet.ServantRetentionPolicy.RETAIN,
            PoaLitePolicySet.RequestProcessingPolicy.USE_ACTIVE_OBJECT_MAP_ONLY,
            PoaLitePolicySet.ImplicitActivationPolicy.NO_IMPLICIT_ACTIVATION));
    assertRejected(
        newPolicy(
            PoaLitePolicySet.ThreadPolicy.ORB_CTRL_MODEL,
            PoaLitePolicySet.LifespanPolicy.TRANSIENT,
            PoaLitePolicySet.IdUniquenessPolicy.MULTIPLE_ID,
            PoaLitePolicySet.IdAssignmentPolicy.SYSTEM_ID,
            PoaLitePolicySet.ServantRetentionPolicy.RETAIN,
            PoaLitePolicySet.RequestProcessingPolicy.USE_ACTIVE_OBJECT_MAP_ONLY,
            PoaLitePolicySet.ImplicitActivationPolicy.NO_IMPLICIT_ACTIVATION));
    assertRejected(
        newPolicy(
            PoaLitePolicySet.ThreadPolicy.ORB_CTRL_MODEL,
            PoaLitePolicySet.LifespanPolicy.TRANSIENT,
            PoaLitePolicySet.IdUniquenessPolicy.UNIQUE_ID,
            PoaLitePolicySet.IdAssignmentPolicy.USER_ID,
            PoaLitePolicySet.ServantRetentionPolicy.RETAIN,
            PoaLitePolicySet.RequestProcessingPolicy.USE_ACTIVE_OBJECT_MAP_ONLY,
            PoaLitePolicySet.ImplicitActivationPolicy.NO_IMPLICIT_ACTIVATION));
    assertRejected(
        newPolicy(
            PoaLitePolicySet.ThreadPolicy.ORB_CTRL_MODEL,
            PoaLitePolicySet.LifespanPolicy.TRANSIENT,
            PoaLitePolicySet.IdUniquenessPolicy.UNIQUE_ID,
            PoaLitePolicySet.IdAssignmentPolicy.SYSTEM_ID,
            PoaLitePolicySet.ServantRetentionPolicy.NON_RETAIN,
            PoaLitePolicySet.RequestProcessingPolicy.USE_ACTIVE_OBJECT_MAP_ONLY,
            PoaLitePolicySet.ImplicitActivationPolicy.NO_IMPLICIT_ACTIVATION));
    assertRejected(
        newPolicy(
            PoaLitePolicySet.ThreadPolicy.ORB_CTRL_MODEL,
            PoaLitePolicySet.LifespanPolicy.TRANSIENT,
            PoaLitePolicySet.IdUniquenessPolicy.UNIQUE_ID,
            PoaLitePolicySet.IdAssignmentPolicy.SYSTEM_ID,
            PoaLitePolicySet.ServantRetentionPolicy.RETAIN,
            PoaLitePolicySet.RequestProcessingPolicy.USE_DEFAULT_SERVANT,
            PoaLitePolicySet.ImplicitActivationPolicy.NO_IMPLICIT_ACTIVATION));
    assertRejected(
        newPolicy(
            PoaLitePolicySet.ThreadPolicy.ORB_CTRL_MODEL,
            PoaLitePolicySet.LifespanPolicy.TRANSIENT,
            PoaLitePolicySet.IdUniquenessPolicy.UNIQUE_ID,
            PoaLitePolicySet.IdAssignmentPolicy.SYSTEM_ID,
            PoaLitePolicySet.ServantRetentionPolicy.RETAIN,
            PoaLitePolicySet.RequestProcessingPolicy.USE_ACTIVE_OBJECT_MAP_ONLY,
            PoaLitePolicySet.ImplicitActivationPolicy.IMPLICIT_ACTIVATION));
  }

  @Test
  void nullPolicyValuesAreRejectedAsBadParam() {
    BAD_PARAM exception =
        assertThrows(
            BAD_PARAM.class,
            () ->
                new PoaLitePolicySet(
                    null,
                    PoaLitePolicySet.LifespanPolicy.TRANSIENT,
                    PoaLitePolicySet.IdUniquenessPolicy.UNIQUE_ID,
                    PoaLitePolicySet.IdAssignmentPolicy.SYSTEM_ID,
                    PoaLitePolicySet.ServantRetentionPolicy.RETAIN,
                    PoaLitePolicySet.RequestProcessingPolicy.USE_ACTIVE_OBJECT_MAP_ONLY,
                    PoaLitePolicySet.ImplicitActivationPolicy.NO_IMPLICIT_ACTIVATION));

    assertEquals(CompletionStatus.COMPLETED_NO, exception.completed);
  }

  private static void assertRejected(ThrowingPolicyFactory factory) {
    BAD_PARAM exception = assertThrows(BAD_PARAM.class, factory::create);

    assertEquals(CompletionStatus.COMPLETED_NO, exception.completed);
  }

  private static ThrowingPolicyFactory newPolicy(
      PoaLitePolicySet.ThreadPolicy threadPolicy,
      PoaLitePolicySet.LifespanPolicy lifespanPolicy,
      PoaLitePolicySet.IdUniquenessPolicy idUniquenessPolicy,
      PoaLitePolicySet.IdAssignmentPolicy idAssignmentPolicy,
      PoaLitePolicySet.ServantRetentionPolicy servantRetentionPolicy,
      PoaLitePolicySet.RequestProcessingPolicy requestProcessingPolicy,
      PoaLitePolicySet.ImplicitActivationPolicy implicitActivationPolicy) {
    return () ->
        new PoaLitePolicySet(
            threadPolicy,
            lifespanPolicy,
            idUniquenessPolicy,
            idAssignmentPolicy,
            servantRetentionPolicy,
            requestProcessingPolicy,
            implicitActivationPolicy);
  }

  @FunctionalInterface
  private interface ThrowingPolicyFactory {

    PoaLitePolicySet create();
  }
}
