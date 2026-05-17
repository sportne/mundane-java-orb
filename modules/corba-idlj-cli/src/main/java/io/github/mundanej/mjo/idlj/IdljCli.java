package io.github.mundanej.mjo.idlj;

import io.github.mundanej.mjo.common.Diagnostic;
import io.github.mundanej.mjo.common.DiagnosticSeverity;
import io.github.mundanej.mjo.idl.parser.IdlParseResult;
import io.github.mundanej.mjo.idl.parser.IdlParser;
import io.github.mundanej.mjo.idl.preprocessor.IdlPreprocessor;
import io.github.mundanej.mjo.idl.preprocessor.IdlSource;
import io.github.mundanej.mjo.idl.preprocessor.PathIdlIncludeResolver;
import io.github.mundanej.mjo.idl.semantics.IdlSemanticAnalyzer;
import io.github.mundanej.mjo.idl.semantics.IdlSemanticResult;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** idlj-like command-line entry point for the approved validation slice. */
public final class IdljCli {

  private final IdljDiagnosticFormatter formatter;

  /** Creates a CLI instance with the default deterministic diagnostic formatter. */
  public IdljCli() {
    this(new IdljDiagnosticFormatter());
  }

  IdljCli(IdljDiagnosticFormatter formatter) {
    this.formatter = Objects.requireNonNull(formatter, "formatter");
  }

  /** Runs the CLI and exits the current process with the returned status. */
  public static void main(String[] args) {
    PrintWriter stdout =
        new PrintWriter(
            new BufferedWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8)));
    PrintWriter stderr =
        new PrintWriter(
            new BufferedWriter(new OutputStreamWriter(System.err, StandardCharsets.UTF_8)));
    int exitCode = new IdljCli().run(args, stdout, stderr);
    System.exit(exitCode);
  }

  /**
   * Runs the CLI using caller-supplied output streams.
   *
   * @return one of {@link IdljExitCodes}
   */
  public int run(String[] args, PrintWriter stdout, PrintWriter stderr) {
    Objects.requireNonNull(args, "args");
    Objects.requireNonNull(stdout, "stdout");
    Objects.requireNonNull(stderr, "stderr");

    try {
      ValidateCommand command = parse(args);
      int exitCode = validate(command, stdout, stderr);
      stdout.flush();
      stderr.flush();
      return exitCode;
    } catch (ArgumentException exception) {
      print(stderr, invalidArguments(exception.getMessage()));
      stdout.flush();
      stderr.flush();
      return IdljExitCodes.USAGE_OR_INPUT_ERROR;
    }
  }

  private int validate(ValidateCommand command, PrintWriter stdout, PrintWriter stderr) {
    IdlParser parser =
        new IdlParser(new IdlPreprocessor(new PathIdlIncludeResolver(command.includePaths())));
    IdlSemanticAnalyzer analyzer = new IdlSemanticAnalyzer();
    boolean inputFailure = false;
    boolean validationFailure = false;

    for (Path sourceFile : command.sourceFiles()) {
      String source;
      try {
        source = Files.readString(sourceFile, StandardCharsets.UTF_8);
      } catch (RuntimeException | java.io.IOException exception) {
        inputFailure = true;
        print(stderr, sourceReadFailed(sourceFile, exception));
        continue;
      }

      IdlParseResult parseResult = parser.parse(new IdlSource(sourceFile.toString(), source));
      printAll(stderr, parseResult.diagnostics());
      if (parseResult.hasErrors()) {
        validationFailure = true;
        continue;
      }

      IdlSemanticResult semanticResult =
          analyzer.analyze(parseResult.translationUnit().orElseThrow());
      printAll(stderr, semanticResult.diagnostics());
      if (semanticResult.hasErrors()) {
        validationFailure = true;
      }
    }

    if (inputFailure) {
      return IdljExitCodes.USAGE_OR_INPUT_ERROR;
    }
    if (validationFailure) {
      return IdljExitCodes.VALIDATION_FAILED;
    }
    if (!command.quiet()) {
      stdout.println("Validated " + command.sourceFiles().size() + " IDL file(s).");
    }
    return IdljExitCodes.SUCCESS;
  }

  private ValidateCommand parse(String[] args) {
    if (args.length == 0) {
      throw new ArgumentException("Missing command; expected 'validate'");
    }
    if (!"validate".equals(args[0])) {
      throw new ArgumentException("Unsupported command: " + args[0]);
    }

    List<Path> includePaths = new ArrayList<>();
    List<Path> sourceFiles = new ArrayList<>();
    boolean quiet = false;
    for (int index = 1; index < args.length; index++) {
      String arg = args[index];
      if ("--quiet".equals(arg)) {
        quiet = true;
      } else if ("--include".equals(arg)) {
        index = requireValue(args, index, "--include");
        includePaths.add(Path.of(args[index]));
      } else if ("-I".equals(arg)) {
        index = requireValue(args, index, "-I");
        includePaths.add(Path.of(args[index]));
      } else if (arg.startsWith("-I") && arg.length() > 2) {
        includePaths.add(Path.of(arg.substring(2)));
      } else if (arg.startsWith("-")) {
        throw new ArgumentException("Unknown option: " + arg);
      } else {
        sourceFiles.add(Path.of(arg));
      }
    }
    if (sourceFiles.isEmpty()) {
      throw new ArgumentException("Missing IDL source file operand");
    }
    return new ValidateCommand(includePaths, sourceFiles, quiet);
  }

  private static int requireValue(String[] args, int index, String option) {
    if (index + 1 >= args.length || args[index + 1].startsWith("-")) {
      throw new ArgumentException("Missing value for " + option);
    }
    return index + 1;
  }

  private void printAll(PrintWriter stderr, List<Diagnostic> diagnostics) {
    for (Diagnostic diagnostic : diagnostics) {
      print(stderr, diagnostic);
    }
  }

  private void print(PrintWriter stderr, Diagnostic diagnostic) {
    stderr.println(formatter.format(diagnostic));
  }

  private static Diagnostic invalidArguments(String message) {
    return Diagnostic.withoutSpan(
        IdljDiagnosticCodes.INVALID_ARGUMENTS, DiagnosticSeverity.ERROR, message);
  }

  private static Diagnostic sourceReadFailed(Path sourceFile, Exception exception) {
    return Diagnostic.withoutSpan(
        IdljDiagnosticCodes.SOURCE_READ_FAILED,
        DiagnosticSeverity.ERROR,
        "Could not read IDL source file: " + sourceFile + " (" + exception.getMessage() + ")");
  }

  private record ValidateCommand(List<Path> includePaths, List<Path> sourceFiles, boolean quiet) {

    private ValidateCommand {
      includePaths = List.copyOf(Objects.requireNonNull(includePaths, "includePaths"));
      sourceFiles = List.copyOf(Objects.requireNonNull(sourceFiles, "sourceFiles"));
    }
  }

  private static final class ArgumentException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private ArgumentException(String message) {
      super(message);
    }
  }
}
