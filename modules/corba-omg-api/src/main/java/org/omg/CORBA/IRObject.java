package org.omg.CORBA;

/** Interface Repository object compatibility surface. */
public interface IRObject extends Object {

  /** Returns the definition kind. */
  DefinitionKind def_kind();

  /** Destroys this repository object. */
  void destroy();
}
