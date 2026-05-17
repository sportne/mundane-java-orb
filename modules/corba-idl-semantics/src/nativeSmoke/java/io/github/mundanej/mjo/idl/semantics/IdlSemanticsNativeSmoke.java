package io.github.mundanej.mjo.idl.semantics;

import io.github.mundanej.mjo.idl.parser.IdlParseResult;
import io.github.mundanej.mjo.idl.parser.IdlParser;
import java.math.BigInteger;

/** Native Image smoke entry point for IDL parser-to-semantics behavior. */
public final class IdlSemanticsNativeSmoke {

  private IdlSemanticsNativeSmoke() {}

  /** Parses and semantically analyzes a compact IDL fixture. */
  public static void main(String[] args) {
    IdlParseResult parseResult =
        new IdlParser()
            .parse(
                "native-semantics-smoke.idl",
                """
                module NativeSmoke {
                  const long BASE = 1 + 2;
                  enum Mode { ON, OFF };
                  const Mode DEFAULT_MODE = Mode::ON;
                  exception Failure { string reason; };
                  interface Service {
                    void ping(in long value) raises (Failure);
                  };
                };
                """);
    if (parseResult.hasErrors()) {
      throw new IllegalStateException("Parse failed: " + parseResult.diagnostics());
    }

    IdlSemanticResult result =
        new IdlSemanticAnalyzer().analyze(parseResult.translationUnit().orElseThrow());
    if (result.hasErrors()) {
      throw new IllegalStateException("Semantic analysis failed: " + result.diagnostics());
    }

    IdlSemanticModel model = result.model().orElseThrow();
    IdlConstantValue.IntegerValue base =
        (IdlConstantValue.IntegerValue)
            model.findSymbol("::NativeSmoke::BASE").orElseThrow().constantValue().orElseThrow();
    IdlConstantValue.EnumeratorValue mode =
        (IdlConstantValue.EnumeratorValue)
            model
                .findSymbol("::NativeSmoke::DEFAULT_MODE")
                .orElseThrow()
                .constantValue()
                .orElseThrow();
    if (!base.value().equals(BigInteger.valueOf(3))
        || !mode.enumeratorName().equals("::NativeSmoke::Mode::ON")
        || model.findSymbol("::NativeSmoke::Service::ping::value").isEmpty()) {
      throw new IllegalStateException("Unexpected semantic smoke model: " + model.symbols());
    }
  }
}
