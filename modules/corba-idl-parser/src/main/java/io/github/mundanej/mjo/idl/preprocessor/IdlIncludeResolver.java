package io.github.mundanej.mjo.idl.preprocessor;

import java.io.IOException;
import java.util.Optional;

/** Resolves IDL include requests to source text without writing files. */
@FunctionalInterface
public interface IdlIncludeResolver {

  /**
   * Resolves an include request.
   *
   * @return the included source, or empty when the include is not available
   * @throws IOException when the resolver cannot read an otherwise selected source
   */
  Optional<IdlSource> resolve(IdlIncludeRequest request) throws IOException;
}
