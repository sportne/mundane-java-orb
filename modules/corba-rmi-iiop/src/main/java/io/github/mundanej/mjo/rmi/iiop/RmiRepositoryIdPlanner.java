package io.github.mundanej.mjo.rmi.iiop;

import io.github.mundanej.mjo.common.Diagnostic;
import io.github.mundanej.mjo.common.DiagnosticCode;
import io.github.mundanej.mjo.common.DiagnosticSeverity;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/** Plans deterministic RMI repository ID strings from Java-to-IDL model metadata. */
public final class RmiRepositoryIdPlanner {

  private static final int MAX_BINARY_NAME_LENGTH = 1_024;
  private static final Pattern JAVA_IDENTIFIER = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");
  private static final Pattern HEX_64 = Pattern.compile("[0-9A-Fa-f]{16}");
  private static final Set<String> JAVA_KEYWORDS =
      Set.of(
          "abstract",
          "assert",
          "boolean",
          "break",
          "byte",
          "case",
          "catch",
          "char",
          "class",
          "const",
          "continue",
          "default",
          "do",
          "double",
          "else",
          "enum",
          "extends",
          "final",
          "finally",
          "float",
          "for",
          "goto",
          "if",
          "implements",
          "import",
          "instanceof",
          "int",
          "interface",
          "long",
          "native",
          "new",
          "package",
          "private",
          "protected",
          "public",
          "return",
          "short",
          "static",
          "strictfp",
          "super",
          "switch",
          "synchronized",
          "this",
          "throw",
          "throws",
          "transient",
          "try",
          "void",
          "volatile",
          "while",
          "_");

  /** Creates a stateless repository ID planner. */
  public RmiRepositoryIdPlanner() {}

  /** Plans repository ID strings for every Java binary name referenced by a mapping model. */
  public RmiRepositoryIdPlanResult plan(
      RmiIdlTranslationUnit translationUnit, List<RmiRepositoryIdHashMetadata> metadata) {
    Objects.requireNonNull(translationUnit, "translationUnit");
    Objects.requireNonNull(metadata, "metadata");
    List<Diagnostic> diagnostics = new ArrayList<>();
    MetadataIndex metadataIndex = collectMetadata(metadata, diagnostics);
    List<String> requiredNames = collectRequiredNames(translationUnit, diagnostics);

    List<RmiRepositoryIdValue> values = new ArrayList<>();
    for (String javaBinaryName : requiredNames) {
      RmiRepositoryIdHashMetadata entry = metadataIndex.metadataByName().get(javaBinaryName);
      if (entry == null) {
        emit(
            diagnostics,
            RmiJavaDiagnosticCodes.MISSING_REPOSITORY_ID_HASH,
            "Missing RMI repository ID hash metadata for " + javaBinaryName);
      } else if (!metadataIndex.invalidNames().contains(entry.javaBinaryName())) {
        values.add(
            new RmiRepositoryIdValue(
                javaBinaryName,
                rmiRepositoryIdValue(
                    entry.javaBinaryName(), entry.hash(), entry.serialVersionUid())));
      }
    }

    if (hasErrors(diagnostics)) {
      return new RmiRepositoryIdPlanResult(Optional.empty(), diagnostics);
    }
    return new RmiRepositoryIdPlanResult(Optional.of(new RmiRepositoryIdPlan(values)), diagnostics);
  }

  private record MetadataIndex(
      Map<String, RmiRepositoryIdHashMetadata> metadataByName, Set<String> invalidNames) {}

  private static MetadataIndex collectMetadata(
      List<RmiRepositoryIdHashMetadata> metadata, List<Diagnostic> diagnostics) {
    Map<String, RmiRepositoryIdHashMetadata> metadataByName = new LinkedHashMap<>();
    Set<String> invalidNames = new LinkedHashSet<>();
    Set<String> duplicates = new LinkedHashSet<>();
    for (RmiRepositoryIdHashMetadata entry : metadata) {
      Objects.requireNonNull(entry, "metadata entry");
      if (!isValidMetadata(entry, diagnostics)) {
        invalidNames.add(entry.javaBinaryName());
      }
      if (metadataByName.putIfAbsent(entry.javaBinaryName(), entry) != null) {
        duplicates.add(entry.javaBinaryName());
      }
    }
    for (String duplicate : duplicates) {
      emit(
          diagnostics,
          RmiJavaDiagnosticCodes.DUPLICATE_REPOSITORY_ID_HASH,
          "Duplicate RMI repository ID hash metadata for " + printable(duplicate));
    }
    return new MetadataIndex(metadataByName, invalidNames);
  }

  private static boolean isValidMetadata(
      RmiRepositoryIdHashMetadata metadata, List<Diagnostic> diagnostics) {
    boolean valid = true;
    if (!isJavaBinaryName(metadata.javaBinaryName())) {
      emit(
          diagnostics,
          RmiJavaDiagnosticCodes.INVALID_REPOSITORY_ID_NAME,
          "Invalid RMI repository ID Java binary name: " + printable(metadata.javaBinaryName()));
      valid = false;
    }
    if (!isHex64(metadata.hash())) {
      emit(
          diagnostics,
          RmiJavaDiagnosticCodes.INVALID_REPOSITORY_ID_HASH,
          "RMI repository ID hash must be exactly 16 hexadecimal digits for "
              + printable(metadata.javaBinaryName()));
      valid = false;
    }
    if (metadata.serialVersionUid().isPresent()
        && !isHex64(metadata.serialVersionUid().orElseThrow())) {
      emit(
          diagnostics,
          RmiJavaDiagnosticCodes.INVALID_REPOSITORY_ID_UID,
          "RMI repository ID serialVersionUID must be exactly 16 hexadecimal digits for "
              + printable(metadata.javaBinaryName()));
      valid = false;
    }
    return valid;
  }

  private static List<String> collectRequiredNames(
      RmiIdlTranslationUnit translationUnit, List<Diagnostic> diagnostics) {
    LinkedHashSet<String> names = new LinkedHashSet<>();
    for (RmiIdlInterface idlInterface : translationUnit.interfaces()) {
      collectInterface(idlInterface, names, diagnostics);
    }
    for (RmiIdlModule module : translationUnit.modules()) {
      collectModule(module, names, diagnostics);
    }
    return List.copyOf(names);
  }

  private static void collectModule(
      RmiIdlModule module, Set<String> names, List<Diagnostic> diagnostics) {
    for (RmiIdlInterface idlInterface : module.interfaces()) {
      collectInterface(idlInterface, names, diagnostics);
    }
    for (RmiIdlModule nestedModule : module.modules()) {
      collectModule(nestedModule, names, diagnostics);
    }
  }

  private static void collectInterface(
      RmiIdlInterface idlInterface, Set<String> names, List<Diagnostic> diagnostics) {
    if (idlInterface.javaBinaryName().isPresent()) {
      addRequiredName(idlInterface.javaBinaryName().orElseThrow(), names);
    } else {
      emit(
          diagnostics,
          RmiJavaDiagnosticCodes.UNRESOLVED_REPOSITORY_ID_MODEL_NAME,
          "IDL interface is missing Java binary-name metadata: " + idlInterface.scopedName());
    }
    for (RmiIdlOperation operation : idlInterface.operations()) {
      collectType(operation.returnType(), names);
      for (RmiIdlParameter parameter : operation.parameters()) {
        collectType(parameter.type(), names);
      }
      for (RmiIdlExceptionReference exception : operation.exceptions()) {
        addRequiredName(exception.javaBinaryName(), names);
      }
    }
  }

  private static void collectType(RmiIdlTypeReference type, Set<String> names) {
    type.javaBinaryName().ifPresent(name -> addRequiredName(name, names));
    type.elementType().ifPresent(elementType -> collectType(elementType, names));
  }

  private static void addRequiredName(String javaBinaryName, Set<String> names) {
    if (!"java.lang.String".equals(javaBinaryName)) {
      names.add(javaBinaryName);
    }
  }

  private static String rmiRepositoryIdValue(
      String javaBinaryName, String hash, Optional<String> serialVersionUid) {
    String normalizedHash = hash.toUpperCase(Locale.ROOT);
    return serialVersionUid
        .map(
            uid ->
                "RMI:" + javaBinaryName + ":" + normalizedHash + ":" + uid.toUpperCase(Locale.ROOT))
        .orElse("RMI:" + javaBinaryName + ":" + normalizedHash);
  }

  private static boolean hasErrors(List<Diagnostic> diagnostics) {
    return diagnostics.stream()
        .anyMatch(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR);
  }

  private static void emit(List<Diagnostic> diagnostics, DiagnosticCode code, String message) {
    diagnostics.add(Diagnostic.withoutSpan(code, DiagnosticSeverity.ERROR, message));
  }

  private static boolean isJavaBinaryName(String value) {
    Objects.requireNonNull(value, "value");
    if (value.isBlank() || value.length() > MAX_BINARY_NAME_LENGTH || value.indexOf(':') >= 0) {
      return false;
    }
    String[] segments = value.split("\\.", -1);
    for (String segment : segments) {
      if (!JAVA_IDENTIFIER.matcher(segment).matches() || JAVA_KEYWORDS.contains(segment)) {
        return false;
      }
    }
    return true;
  }

  private static boolean isHex64(String value) {
    Objects.requireNonNull(value, "value");
    return HEX_64.matcher(value).matches();
  }

  private static String printable(String value) {
    return value.isBlank() ? "<blank>" : value;
  }
}
