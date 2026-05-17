/**
 * Minimal POA-lite servant dispatch for generated-style local invocation.
 *
 * <p>This package implements only the G6-620 approved profile: {@code ORB_CTRL_MODEL}, {@code
 * TRANSIENT}, {@code UNIQUE_ID}, {@code SYSTEM_ID}, {@code RETAIN}, {@code
 * USE_ACTIVE_OBJECT_MAP_ONLY}, and {@code NO_IMPLICIT_ACTIVATION}. It is an in-process
 * active-object-map dispatch layer over {@code LocalOrb}; full POA policy behavior remains a later
 * task.
 */
package io.github.mundanej.mjo.poa;
