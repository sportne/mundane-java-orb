/**
 * Deterministic Java source rendering for the minimal IDL-to-Java generation slice.
 *
 * <p>This package renders source text only. The first descriptor slice emits static descriptor
 * source and compile-only codec surfaces, but no functional CDR marshaling, runtime bindings,
 * reflection metadata, stubs, skeletons, helpers, holders, or POA artifacts.
 *
 * <p>RMI-specific binding surfaces are emitted by {@code corba-rmi-iiop}; this generic IDL renderer
 * only validates that approved generated RMI IDL continues through the normal IDL-to-Java path.
 */
package io.github.mundanej.mjo.codegen;
