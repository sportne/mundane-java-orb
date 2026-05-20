/**
 * Deterministic compile-safe IDL-to-Java mapping model for the minimal G6 compiler slice.
 *
 * <p>The mapping records source-generation decisions only. It does not define CORBA runtime APIs,
 * CDR codecs, helpers, holders, stubs, skeletons, POA classes, or Native Image metadata.
 *
 * <p>RMI-specific binding surfaces are modeled and emitted by {@code corba-rmi-iiop}; this package
 * only validates that approved generated RMI IDL maps through the normal IDL-to-Java path.
 */
package io.github.mundanej.mjo.idl.java.mapping;
