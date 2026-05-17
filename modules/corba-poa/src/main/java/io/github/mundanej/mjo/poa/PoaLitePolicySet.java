package io.github.mundanej.mjo.poa;

/**
 * Fixed POA-lite policy profile approved for G6-620.
 *
 * @param threadPolicy thread dispatch policy
 * @param lifespanPolicy object-reference lifespan policy
 * @param idUniquenessPolicy servant/object-id uniqueness policy
 * @param idAssignmentPolicy object-id assignment policy
 * @param servantRetentionPolicy servant retention policy
 * @param requestProcessingPolicy servant lookup policy
 * @param implicitActivationPolicy implicit activation policy
 */
public record PoaLitePolicySet(
    ThreadPolicy threadPolicy,
    LifespanPolicy lifespanPolicy,
    IdUniquenessPolicy idUniquenessPolicy,
    IdAssignmentPolicy idAssignmentPolicy,
    ServantRetentionPolicy servantRetentionPolicy,
    RequestProcessingPolicy requestProcessingPolicy,
    ImplicitActivationPolicy implicitActivationPolicy) {

  /** Creates a validated POA-lite policy set. */
  public PoaLitePolicySet {
    threadPolicy = PoaLiteExceptions.requireNonNull(threadPolicy, "threadPolicy");
    lifespanPolicy = PoaLiteExceptions.requireNonNull(lifespanPolicy, "lifespanPolicy");
    idUniquenessPolicy = PoaLiteExceptions.requireNonNull(idUniquenessPolicy, "idUniquenessPolicy");
    idAssignmentPolicy = PoaLiteExceptions.requireNonNull(idAssignmentPolicy, "idAssignmentPolicy");
    servantRetentionPolicy =
        PoaLiteExceptions.requireNonNull(servantRetentionPolicy, "servantRetentionPolicy");
    requestProcessingPolicy =
        PoaLiteExceptions.requireNonNull(requestProcessingPolicy, "requestProcessingPolicy");
    implicitActivationPolicy =
        PoaLiteExceptions.requireNonNull(implicitActivationPolicy, "implicitActivationPolicy");
    if (threadPolicy != ThreadPolicy.ORB_CTRL_MODEL
        || lifespanPolicy != LifespanPolicy.TRANSIENT
        || idUniquenessPolicy != IdUniquenessPolicy.UNIQUE_ID
        || idAssignmentPolicy != IdAssignmentPolicy.SYSTEM_ID
        || servantRetentionPolicy != ServantRetentionPolicy.RETAIN
        || requestProcessingPolicy != RequestProcessingPolicy.USE_ACTIVE_OBJECT_MAP_ONLY
        || implicitActivationPolicy != ImplicitActivationPolicy.NO_IMPLICIT_ACTIVATION) {
      throw PoaLiteExceptions.badParam(
          "POA-lite supports only ORB_CTRL_MODEL, TRANSIENT, UNIQUE_ID, SYSTEM_ID, "
              + "RETAIN, USE_ACTIVE_OBJECT_MAP_ONLY, and NO_IMPLICIT_ACTIVATION");
    }
  }

  /** Returns the approved POA-lite policy profile. */
  public static PoaLitePolicySet approvedProfile() {
    return new PoaLitePolicySet(
        ThreadPolicy.ORB_CTRL_MODEL,
        LifespanPolicy.TRANSIENT,
        IdUniquenessPolicy.UNIQUE_ID,
        IdAssignmentPolicy.SYSTEM_ID,
        ServantRetentionPolicy.RETAIN,
        RequestProcessingPolicy.USE_ACTIVE_OBJECT_MAP_ONLY,
        ImplicitActivationPolicy.NO_IMPLICIT_ACTIVATION);
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
