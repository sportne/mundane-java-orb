/**
 * Bounded local models for the supported CORBA Security Service / CSIv2 subset.
 *
 * <p>The current G8-610 through G8-660 slices provide explicit credential/trust primitives, bounded
 * policy validation, deterministic project-owned CSIv2 metadata encode/decode, local policy
 * evaluation decisions, redacted audit/failure disclosure models, and descriptor-backed loopback
 * IIOP service-context/tagged-component handling. Native Image smoke, interop metadata, secret
 * discovery, automatic TLS policy changes, and global JVM security state are outside this package's
 * implemented scope.
 */
package io.github.mundanej.mjo.security;
