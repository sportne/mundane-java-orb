package io.github.mundanej.mjo.transaction;

import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.Base64;

/** Deterministic text codec for local Transaction Service propagation metadata. */
public final class TransactionPropagationCodec {

  /** Stable codec version prefix for the project-owned context encoding. */
  public static final String VERSION = "mjo-txn-prop-v1";

  /** Upper bound for encoded propagation context text. */
  public static final int MAX_ENCODED_LENGTH = 512;

  private static final int FIELD_COUNT = 5;
  private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

  /** Encodes a propagation context into bounded deterministic text. */
  public String encode(TransactionPropagationContext context) {
    String encoded =
        VERSION
            + '|'
            + ENCODER.encodeToString(
                context.transactionId().value().getBytes(StandardCharsets.UTF_8))
            + '|'
            + context.transactionGeneration()
            + '|'
            + context.beganAt()
            + '|'
            + context.expiresAt();
    if (encoded.length() > MAX_ENCODED_LENGTH) {
      throw new TransactionServiceException(
          TransactionServiceDiagnosticCodes.MALFORMED_PROPAGATION_CONTEXT,
          "propagation context exceeds " + MAX_ENCODED_LENGTH + " characters");
    }
    return encoded;
  }

  /** Decodes bounded deterministic text into an immutable propagation context. */
  public TransactionPropagationContext decode(String encoded) {
    if (encoded == null || encoded.isBlank()) {
      throw malformed("propagation context must not be blank");
    }
    if (encoded.length() > MAX_ENCODED_LENGTH) {
      throw malformed("propagation context exceeds " + MAX_ENCODED_LENGTH + " characters");
    }
    String[] fields = encoded.split("\\|", -1);
    if (fields.length != FIELD_COUNT) {
      throw malformed("propagation context must contain " + FIELD_COUNT + " fields");
    }
    if (!VERSION.equals(fields[0])) {
      throw malformed("unsupported propagation context version");
    }
    try {
      String transactionId = new String(DECODER.decode(fields[1]), StandardCharsets.UTF_8);
      long generation = Long.parseLong(fields[2]);
      Instant beganAt = Instant.parse(fields[3]);
      Instant expiresAt = Instant.parse(fields[4]);
      return new TransactionPropagationContext(
          new TransactionId(transactionId), generation, beganAt, expiresAt);
    } catch (DateTimeException | IllegalArgumentException | TransactionServiceException exception) {
      throw malformed("malformed propagation context field");
    }
  }

  private static TransactionServiceException malformed(String message) {
    return new TransactionServiceException(
        TransactionServiceDiagnosticCodes.MALFORMED_PROPAGATION_CONTEXT, message);
  }
}
