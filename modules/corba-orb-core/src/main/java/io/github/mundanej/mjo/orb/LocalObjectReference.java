package io.github.mundanej.mjo.orb;

import io.github.mundanej.mjo.typecode.IdlGeneratedTypeDescriptor;
import java.util.Objects;

/** Immutable local object reference created by {@link LocalOrb}. */
public final class LocalObjectReference<T> {

  private final long ownerToken;
  private final String objectId;
  private final Class<T> javaType;
  private final IdlGeneratedTypeDescriptor descriptor;

  LocalObjectReference(
      long ownerToken, String objectId, Class<T> javaType, IdlGeneratedTypeDescriptor descriptor) {
    this.ownerToken = ownerToken;
    this.objectId = requireNonBlank(objectId, "objectId");
    this.javaType = Objects.requireNonNull(javaType, "javaType");
    this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
  }

  /** Returns the deterministic per-ORB local object id. */
  public String objectId() {
    return objectId;
  }

  /** Returns the generated Java interface type associated with this reference. */
  public Class<T> javaType() {
    return javaType;
  }

  /** Returns the static IDL descriptor associated with this reference. */
  public IdlGeneratedTypeDescriptor descriptor() {
    return descriptor;
  }

  long ownerToken() {
    return ownerToken;
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof LocalObjectReference<?> that)) {
      return false;
    }
    return ownerToken == that.ownerToken && objectId.equals(that.objectId);
  }

  @Override
  public int hashCode() {
    return 31 * Long.hashCode(ownerToken) + objectId.hashCode();
  }

  @Override
  public String toString() {
    return "LocalObjectReference[objectId=" + objectId + ", javaType=" + javaType.getName() + "]";
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
