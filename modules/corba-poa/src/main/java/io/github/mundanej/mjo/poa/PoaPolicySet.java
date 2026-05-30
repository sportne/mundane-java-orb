package io.github.mundanej.mjo.poa;

/**
 * Validated local POA policy set for the G6 full-POA expansion.
 *
 * @param threadPolicy thread dispatch policy
 * @param lifespanPolicy object-reference lifespan policy
 * @param idUniquenessPolicy servant/object-id uniqueness policy
 * @param idAssignmentPolicy object-id assignment policy
 * @param servantRetentionPolicy servant retention policy
 * @param requestProcessingPolicy servant lookup policy
 * @param implicitActivationPolicy implicit activation policy
 */
public record PoaPolicySet(
    ThreadPolicy threadPolicy,
    LifespanPolicy lifespanPolicy,
    IdUniquenessPolicy idUniquenessPolicy,
    IdAssignmentPolicy idAssignmentPolicy,
    ServantRetentionPolicy servantRetentionPolicy,
    RequestProcessingPolicy requestProcessingPolicy,
    ImplicitActivationPolicy implicitActivationPolicy) {

  /** Creates a validated local POA policy set. */
  public PoaPolicySet {
    threadPolicy = PoaExceptions.requireNonNull(threadPolicy, "threadPolicy");
    lifespanPolicy = PoaExceptions.requireNonNull(lifespanPolicy, "lifespanPolicy");
    idUniquenessPolicy = PoaExceptions.requireNonNull(idUniquenessPolicy, "idUniquenessPolicy");
    idAssignmentPolicy = PoaExceptions.requireNonNull(idAssignmentPolicy, "idAssignmentPolicy");
    servantRetentionPolicy =
        PoaExceptions.requireNonNull(servantRetentionPolicy, "servantRetentionPolicy");
    requestProcessingPolicy =
        PoaExceptions.requireNonNull(requestProcessingPolicy, "requestProcessingPolicy");
    implicitActivationPolicy =
        PoaExceptions.requireNonNull(implicitActivationPolicy, "implicitActivationPolicy");
    validateLocalPolicy(
        idAssignmentPolicy,
        servantRetentionPolicy,
        requestProcessingPolicy,
        implicitActivationPolicy);
  }

  /** Returns the POA-lite-compatible retained active-object-map profile. */
  public static PoaPolicySet poaLiteProfile() {
    return new PoaPolicySet(
        ThreadPolicy.ORB_CTRL_MODEL,
        LifespanPolicy.TRANSIENT,
        IdUniquenessPolicy.UNIQUE_ID,
        IdAssignmentPolicy.SYSTEM_ID,
        ServantRetentionPolicy.RETAIN,
        RequestProcessingPolicy.USE_ACTIVE_OBJECT_MAP_ONLY,
        ImplicitActivationPolicy.NO_IMPLICIT_ACTIVATION);
  }

  /** Returns a transient POA profile that uses a retained active object map. */
  public static PoaPolicySet transientRetainedProfile() {
    return poaLiteProfile();
  }

  private static void validateLocalPolicy(
      IdAssignmentPolicy idAssignmentPolicy,
      ServantRetentionPolicy servantRetentionPolicy,
      RequestProcessingPolicy requestProcessingPolicy,
      ImplicitActivationPolicy implicitActivationPolicy) {
    if (servantRetentionPolicy == ServantRetentionPolicy.NON_RETAIN
        && requestProcessingPolicy == RequestProcessingPolicy.USE_ACTIVE_OBJECT_MAP_ONLY) {
      throw PoaExceptions.badParam("NON_RETAIN cannot use USE_ACTIVE_OBJECT_MAP_ONLY");
    }
    if (implicitActivationPolicy == ImplicitActivationPolicy.IMPLICIT_ACTIVATION
        && (idAssignmentPolicy != IdAssignmentPolicy.SYSTEM_ID
            || servantRetentionPolicy != ServantRetentionPolicy.RETAIN)) {
      throw PoaExceptions.badParam(
          "IMPLICIT_ACTIVATION requires SYSTEM_ID and RETAIN POA policies");
    }
  }

  /** Thread dispatch policy. */
  public enum ThreadPolicy {
    ORB_CTRL_MODEL,
    SINGLE_THREAD_MODEL
  }

  /** Object-reference lifespan policy. */
  public enum LifespanPolicy {
    TRANSIENT,
    PERSISTENT
  }

  /** Servant/object-id uniqueness policy. */
  public enum IdUniquenessPolicy {
    UNIQUE_ID,
    MULTIPLE_ID
  }

  /** Object-id assignment policy. */
  public enum IdAssignmentPolicy {
    USER_ID,
    SYSTEM_ID
  }

  /** Servant retention policy. */
  public enum ServantRetentionPolicy {
    RETAIN,
    NON_RETAIN
  }

  /** Request processing policy. */
  public enum RequestProcessingPolicy {
    USE_ACTIVE_OBJECT_MAP_ONLY,
    USE_DEFAULT_SERVANT,
    USE_SERVANT_MANAGER
  }

  /** Implicit activation policy. */
  public enum ImplicitActivationPolicy {
    IMPLICIT_ACTIVATION,
    NO_IMPLICIT_ACTIVATION
  }
}
