/**
 * Bounded local models for the supported CORBA Security Service / CSIv2 subset.
 *
 * <p>The current G8-610 through G8-640 slices provide explicit credential/trust primitives, bounded
 * policy validation, deterministic project-owned CSIv2 metadata encode/decode, and local policy
 * evaluation decisions. Audit disclosure, IIOP integration, Native Image smoke, interop metadata,
 * secret discovery, and global JVM security state are outside this package's implemented scope.
 */
package io.github.mundanej.mjo.security;
