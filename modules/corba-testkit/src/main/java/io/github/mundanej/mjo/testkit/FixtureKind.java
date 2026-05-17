package io.github.mundanej.mjo.testkit;

/** Supported fixture categories for reusable verification assets. */
public enum FixtureKind {
  /** IDL source input fixture. */
  IDL,
  /** Golden generated-source fixture. */
  GOLDEN_SOURCE,
  /** Golden wire-byte fixture. */
  GOLDEN_WIRE
}
