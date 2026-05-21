package io.github.mundanej.mjo.rmi.iiop;

import io.github.mundanej.mjo.orb.LocalObjectReference;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/** Deterministic bounded GIOP KeyAddr object key for local RMI-IIOP wire scenarios. */
public final class RmiIiopObjectKey {

  /** Maximum object-key octets accepted by the approved G7-080 slice. */
  public static final int MAX_OCTETS = 256;

  private final byte[] bytes;

  private RmiIiopObjectKey(byte[] bytes) {
    this.bytes = validate(bytes);
  }

  /** Creates an object key from a local object reference's deterministic object id. */
  public static RmiIiopObjectKey forLocalObjectReference(LocalObjectReference<?> reference) {
    Objects.requireNonNull(reference, "reference");
    return fromString(reference.objectId());
  }

  /** Creates an object key from a nonblank UTF-8 string. */
  public static RmiIiopObjectKey fromString(String value) {
    Objects.requireNonNull(value, "value");
    if (value.isBlank()) {
      throw invalid("RMI-IIOP object key must not be blank");
    }
    return new RmiIiopObjectKey(value.getBytes(StandardCharsets.UTF_8));
  }

  /** Creates an object key from already encoded KeyAddr octets. */
  public static RmiIiopObjectKey fromBytes(byte[] bytes) {
    return new RmiIiopObjectKey(bytes);
  }

  /** Returns a defensive copy of this object key's KeyAddr octets. */
  public byte[] bytes() {
    return Arrays.copyOf(bytes, bytes.length);
  }

  /** Returns the UTF-8 diagnostic spelling of this object key. */
  public String value() {
    return new String(bytes, StandardCharsets.UTF_8);
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof RmiIiopObjectKey that && Arrays.equals(bytes, that.bytes);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(bytes);
  }

  @Override
  public String toString() {
    return value();
  }

  private static byte[] validate(byte[] input) {
    Objects.requireNonNull(input, "bytes");
    if (input.length == 0) {
      throw invalid("RMI-IIOP object key must not be empty");
    }
    if (input.length > MAX_OCTETS) {
      throw invalid("RMI-IIOP object key exceeds " + MAX_OCTETS + " octets: " + input.length);
    }
    return Arrays.copyOf(input, input.length);
  }

  private static RmiIiopWireException invalid(String message) {
    return new RmiIiopWireException(RmiJavaDiagnosticCodes.INVALID_WIRE_OBJECT_KEY, message);
  }
}
