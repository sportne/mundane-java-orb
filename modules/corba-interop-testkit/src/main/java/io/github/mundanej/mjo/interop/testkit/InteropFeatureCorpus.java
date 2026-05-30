package io.github.mundanej.mjo.interop.testkit;

import java.util.List;

/** Source-controlled IDL feature corpus identities used by local interop lanes. */
public final class InteropFeatureCorpus {

  private static final List<InteropScenario> G12_WIDE =
      List.of(
          new InteropScenario("g12-wide-core-types", "interop/idl/g12-wide/CoreTypes.idl"),
          new InteropScenario(
              "g12-wide-repository-pragmas", "interop/idl/g12-wide/RepositoryPragmas.idl"),
          new InteropScenario("g12-wide-valuetypes", "interop/idl/g12-wide/ValueTypes.idl"));

  private static final InteropScenario G12_UNSUPPORTED_CUSTOM_VALUE =
      new InteropScenario(
          "g12-wide-unsupported-custom-value", "interop/idl/g12-wide/UnsupportedCustomValue.idl");

  private InteropFeatureCorpus() {}

  /** Returns G12 broad-feature fixtures that must parse, analyze, map, and compile locally. */
  public static List<InteropScenario> g12Wide() {
    return G12_WIDE;
  }

  /** Returns the G12 custom-valuetype fixture that is intentionally unsupported by mapping. */
  public static InteropScenario g12UnsupportedCustomValue() {
    return G12_UNSUPPORTED_CUSTOM_VALUE;
  }
}
