/**
 * RMI-IIOP and Java-to-IDL planning surfaces.
 *
 * <p>This package currently exposes explicit declaration models, deterministic eligibility
 * diagnostics, an in-memory Java-to-IDL model, and explicit metadata-based RMI repository ID
 * planning, plus deterministic generated IDL fixtures and compile-safe Java binding source surfaces
 * for the approved subset. It also exposes bounded local CDR codecs for approved primitive/String
 * values and empty declared user-exception payloads by repository ID, and generated local ORB/POA
 * adapter surfaces for the approved binding slice. It does not load application classes, inspect
 * classpaths, compute Java serialization hashes, or perform IIOP wire behavior.
 */
package io.github.mundanej.mjo.rmi.iiop;
