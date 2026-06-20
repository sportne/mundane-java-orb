package io.github.mundanej.mjo.transaction;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Codec for Transaction Service propagation metadata carried in IIOP request contexts. */
public final class TransactionIiopRequestContextCodec {

  /** Maximum request-context descriptors accepted by this transaction-owned boundary. */
  public static final int MAX_REQUEST_CONTEXTS = 64;

  private final TransactionPropagationCodec propagationCodec;

  /** Creates a codec backed by the default local propagation codec. */
  public TransactionIiopRequestContextCodec() {
    this(new TransactionPropagationCodec());
  }

  /** Creates a codec backed by a caller-provided local propagation codec. */
  public TransactionIiopRequestContextCodec(TransactionPropagationCodec propagationCodec) {
    this.propagationCodec = Objects.requireNonNull(propagationCodec, "propagationCodec");
  }

  /** Encodes local propagation metadata as a bounded IIOP request-context descriptor. */
  public TransactionIiopRequestContextDescriptor encode(TransactionPropagationContext context) {
    String encoded = propagationCodec.encode(context);
    return new TransactionIiopRequestContextDescriptor(
        TransactionIiopRequestContextDescriptor.TRANSACTION_SERVICE_CONTEXT_ID,
        encoded.getBytes(StandardCharsets.UTF_8));
  }

  /** Decodes local propagation metadata from a transaction request-context descriptor. */
  public TransactionPropagationContext decode(TransactionIiopRequestContextDescriptor descriptor) {
    Objects.requireNonNull(descriptor, "descriptor");
    if (!descriptor.isTransactionServiceContext()) {
      throw malformed("request context is not a Transaction Service context");
    }
    try {
      String encoded =
          StandardCharsets.UTF_8
              .newDecoder()
              .onMalformedInput(CodingErrorAction.REPORT)
              .onUnmappableCharacter(CodingErrorAction.REPORT)
              .decode(ByteBuffer.wrap(descriptor.contextData()))
              .toString();
      return propagationCodec.decode(encoded);
    } catch (CharacterCodingException | TransactionServiceException exception) {
      throw malformed("malformed Transaction Service request context");
    }
  }

  /** Finds the single transaction request context in a bounded request-context list. */
  public Optional<TransactionIiopRequestContextDescriptor> findTransactionContext(
      List<TransactionIiopRequestContextDescriptor> descriptors) {
    Objects.requireNonNull(descriptors, "descriptors");
    if (descriptors.size() > MAX_REQUEST_CONTEXTS) {
      throw malformed("request context list exceeds " + MAX_REQUEST_CONTEXTS + " entries");
    }
    TransactionIiopRequestContextDescriptor found = null;
    for (TransactionIiopRequestContextDescriptor descriptor : descriptors) {
      Objects.requireNonNull(descriptor, "descriptor");
      if (descriptor.isTransactionServiceContext()) {
        if (found != null) {
          throw malformed("duplicate Transaction Service request context");
        }
        found = descriptor;
      }
    }
    return Optional.ofNullable(found);
  }

  private static TransactionServiceException malformed(String message) {
    return new TransactionServiceException(
        TransactionServiceDiagnosticCodes.MALFORMED_REQUEST_CONTEXT, message);
  }
}
