package io.github.mundanej.mjo.orb;

import io.github.mundanej.mjo.typecode.IdlGeneratedTypeDescriptor;
import java.util.Objects;
import java.util.Optional;

/** Immutable local object reference created by {@link LocalOrb}. */
public final class LocalObjectReference<T> {

  private final long ownerToken;
  private final String bindingId;
  private final String objectId;
  private final Class<T> javaType;
  private final IdlGeneratedTypeDescriptor descriptor;
  private final DurableObjectKey durableObjectKey;

  LocalObjectReference(
      long ownerToken, String objectId, Class<T> javaType, IdlGeneratedTypeDescriptor descriptor) {
    this(ownerToken, objectId, javaType, descriptor, null);
  }

  LocalObjectReference(
      long ownerToken,
      String objectId,
      Class<T> javaType,
      IdlGeneratedTypeDescriptor descriptor,
      DurableObjectKey durableObjectKey) {
    this(ownerToken, objectId, objectId, javaType, descriptor, durableObjectKey);
  }

  LocalObjectReference(
      long ownerToken,
      String bindingId,
      String objectId,
      Class<T> javaType,
      IdlGeneratedTypeDescriptor descriptor,
      DurableObjectKey durableObjectKey) {
    this.ownerToken = ownerToken;
    this.bindingId = requireNonBlank(bindingId, "bindingId");
    this.objectId = requireNonBlank(objectId, "objectId");
    this.javaType = Objects.requireNonNull(javaType, "javaType");
    this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
    this.durableObjectKey = durableObjectKey;
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

  /** Returns persistent POA key metadata when this reference was created by a durable POA. */
  public Optional<DurableObjectKey> durableObjectKey() {
    return Optional.ofNullable(durableObjectKey);
  }

  long ownerToken() {
    return ownerToken;
  }

  String bindingId() {
    return bindingId;
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof LocalObjectReference<?> that)) {
      return false;
    }
    return ownerToken == that.ownerToken && bindingId.equals(that.bindingId);
  }

  @Override
  public int hashCode() {
    return 31 * Long.hashCode(ownerToken) + bindingId.hashCode();
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
