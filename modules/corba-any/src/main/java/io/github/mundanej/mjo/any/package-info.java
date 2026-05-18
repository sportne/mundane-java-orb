/**
 * Local, static-descriptor-backed Any support.
 *
 * <p>The package stores local Any values as a TypeCode plus payload and encodes/decodes supported
 * payloads through explicit CDR codecs. Full CORBA wire TypeCode marshaling and dynamic invocation
 * are intentionally outside this package.
 */
package io.github.mundanej.mjo.any;
