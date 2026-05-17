package io.github.mundanej.mjo.idl.ast;

import io.github.mundanej.mjo.common.SourceSpan;

/** Common contract for immutable IDL AST nodes. */
public interface IdlAstNode {

  /** Returns the source span covered by this node. */
  SourceSpan span();
}
