package io.github.mundanej.mjo.iiop;

import io.github.mundanej.mjo.orb.LocalObjectReference;

/** Resolves opaque IIOP object-key octets into a local object reference. */
@FunctionalInterface
public interface IiopDurableObjectResolver {

  /**
   * Resolves one opaque object key.
   *
   * <p>The resolver owns durable-key decoding and diagnostics. Protocol code must not parse
   * project-owned durable key formats.
   */
  LocalObjectReference<?> resolve(byte[] objectKey);
}
