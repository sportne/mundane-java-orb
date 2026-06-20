package io.github.mundanej.mjo.transaction;

import java.util.Arrays;
import java.util.Objects;

/** Immutable descriptor for an IIOP service context carrying transaction metadata. */
public final class TransactionIiopRequestContextDescriptor {

  /** Project-owned IIOP service-context ID for the supported local transaction subset. */
  public static final long TRANSACTION_SERVICE_CONTEXT_ID = 0x4D4A5458L;

  private final long contextId;
  private final byte[] contextData;

  /** Creates a bounded request-context descriptor. */
  public TransactionIiopRequestContextDescriptor(long contextId, byte[] contextData) {
    if (contextId < 0 || contextId > 0xFFFF_FFFFL) {
      throw malformed("request context ID must fit in unsigned 32-bit range");
    }
    byte[] copy = Objects.requireNonNull(contextData, "contextData").clone();
    if (copy.length == 0) {
      throw malformed("transaction request context data must not be empty");
    }
    if (copy.length > TransactionPropagationCodec.MAX_ENCODED_LENGTH) {
      throw malformed(
          "transaction request context exceeds "
              + TransactionPropagationCodec.MAX_ENCODED_LENGTH
              + " bytes");
    }
    this.contextId = contextId;
    this.contextData = copy;
  }

  /** Returns the IIOP service-context ID. */
  public long contextId() {
    return contextId;
  }

  /** Returns a defensive copy of the service-context bytes. */
  public byte[] contextData() {
    return contextData.clone();
  }

  /** Returns whether this descriptor carries local Transaction Service propagation metadata. */
  public boolean isTransactionServiceContext() {
    return contextId == TRANSACTION_SERVICE_CONTEXT_ID;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof TransactionIiopRequestContextDescriptor descriptor
        && contextId == descriptor.contextId
        && Arrays.equals(contextData, descriptor.contextData);
  }

  @Override
  public int hashCode() {
    return 31 * Long.hashCode(contextId) + Arrays.hashCode(contextData);
  }

  @Override
  public String toString() {
    return "TransactionIiopRequestContextDescriptor[contextId="
        + contextId
        + ", contextDataLength="
        + contextData.length
        + ']';
  }

  private static TransactionServiceException malformed(String message) {
    return new TransactionServiceException(
        TransactionServiceDiagnosticCodes.MALFORMED_REQUEST_CONTEXT, message);
  }
}
