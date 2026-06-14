package io.github.mundanej.mjo.trading;

/** Supported metadata directions for local Trading Service import/export links. */
public enum TradingImportExportDirection {
  /** Metadata for an import link that would query another trader in a future federation slice. */
  IMPORT,

  /** Metadata for an export link that would expose local offers in a future federation slice. */
  EXPORT
}
