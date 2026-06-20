package io.github.mundanej.mjo.transaction;

enum NoOpTransactionResourceParticipant implements TransactionResourceParticipant {
  INSTANCE;

  @Override
  public TransactionResourceVote prepare() {
    return TransactionResourceVote.COMMIT;
  }

  @Override
  public void commit() {}

  @Override
  public void rollback() {}
}
