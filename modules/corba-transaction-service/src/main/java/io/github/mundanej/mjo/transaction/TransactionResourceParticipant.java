package io.github.mundanej.mjo.transaction;

/** Local resource callbacks used by the bounded Transaction Service coordinator. */
public interface TransactionResourceParticipant {

  /** Returns this resource's local prepare vote. */
  TransactionResourceVote prepare();

  /** Applies this resource's local commit callback. */
  void commit();

  /** Applies this resource's local rollback callback. */
  void rollback();

  /** Returns a participant whose callbacks always complete successfully. */
  static TransactionResourceParticipant noOp() {
    return NoOpTransactionResourceParticipant.INSTANCE;
  }
}
