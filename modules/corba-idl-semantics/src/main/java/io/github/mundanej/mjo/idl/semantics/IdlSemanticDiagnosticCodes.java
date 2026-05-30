package io.github.mundanej.mjo.idl.semantics;

import io.github.mundanej.mjo.common.DiagnosticCode;

/** Stable diagnostic codes emitted by IDL semantic analysis. */
public final class IdlSemanticDiagnosticCodes {

  /** A declaration reuses a name that already exists in the same semantic scope. */
  public static final DiagnosticCode DUPLICATE_NAME = new DiagnosticCode("IDL-0400");

  /** A scoped name or relative name cannot be resolved. */
  public static final DiagnosticCode UNRESOLVED_NAME = new DiagnosticCode("IDL-0401");

  /** A name resolved successfully but is not valid for the required type position. */
  public static final DiagnosticCode INVALID_TYPE_REFERENCE = new DiagnosticCode("IDL-0402");

  /** A constant expression is malformed or uses unsupported expression syntax. */
  public static final DiagnosticCode INVALID_CONSTANT_EXPRESSION = new DiagnosticCode("IDL-0403");

  /** A constant value is not valid for the declared constant type. */
  public static final DiagnosticCode INVALID_CONSTANT_VALUE = new DiagnosticCode("IDL-0404");

  /** A raises clause names something other than a previously declared exception. */
  public static final DiagnosticCode INVALID_RAISES_TARGET = new DiagnosticCode("IDL-0405");

  /** A reference differs from the defining identifier only by case. */
  public static final DiagnosticCode CASE_MISMATCH = new DiagnosticCode("IDL-0406");

  /** An interface inheritance list is invalid or cyclic. */
  public static final DiagnosticCode INVALID_INHERITANCE = new DiagnosticCode("IDL-0407");

  /** A union case label is invalid for its discriminator. */
  public static final DiagnosticCode INVALID_UNION_LABEL = new DiagnosticCode("IDL-0408");

  /** A forward declaration has no matching complete declaration or has incompatible modifiers. */
  public static final DiagnosticCode INVALID_FORWARD_DECLARATION = new DiagnosticCode("IDL-0409");

  /** A value-recursive type graph is illegal for by-value IDL members. */
  public static final DiagnosticCode INVALID_RECURSIVE_TYPE = new DiagnosticCode("IDL-0410");

  /** A repository-affecting pragma is malformed or conflicts with existing metadata. */
  public static final DiagnosticCode INVALID_REPOSITORY_PRAGMA = new DiagnosticCode("IDL-0411");

  /** An operation context clause is semantically invalid. */
  public static final DiagnosticCode INVALID_OPERATION_CONTEXT = new DiagnosticCode("IDL-0412");

  /** A name inherited through multiple base scopes is ambiguous. */
  public static final DiagnosticCode AMBIGUOUS_NAME = new DiagnosticCode("IDL-0413");

  private IdlSemanticDiagnosticCodes() {}
}
