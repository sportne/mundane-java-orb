/**
 * RMI-IIOP and Java-to-IDL planning surfaces.
 *
 * <p>This package currently exposes explicit declaration models, deterministic eligibility
 * diagnostics, an in-memory Java-to-IDL model, and explicit metadata-based RMI repository ID
 * planning, plus deterministic generated IDL fixtures and compile-safe Java binding source surfaces
 * for the approved subset. It also exposes bounded local CDR codecs for approved primitive/String
 * values and empty declared user-exception payloads by repository ID, and generated local ORB/POA
 * adapter surfaces for the approved binding slice. G7-080 adds bounded local JVM RMI-IIOP wire
 * integration over explicit object keys, existing GIOP request/reply messages, and existing IIOP
 * client/server paths. It does not load application classes, inspect classpaths, compute Java
 * serialization hashes, or claim external peer interoperability.
 */
package io.github.mundanej.mjo.rmi.iiop;
