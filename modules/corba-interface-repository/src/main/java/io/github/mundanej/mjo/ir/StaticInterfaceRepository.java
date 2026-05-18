package io.github.mundanej.mjo.ir;

import io.github.mundanej.mjo.repositoryid.RepositoryId;
import io.github.mundanej.mjo.typecode.IdlGeneratedTypeDescriptor;
import io.github.mundanej.mjo.typecode.IdlOperationDescriptor;
import io.github.mundanej.mjo.typecode.IdlTypeCode;
import io.github.mundanej.mjo.typecode.IdlTypeKind;
import io.github.mundanej.mjo.typecode.IdlTypeReference;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable local Interface Repository backed by explicit generated descriptors. */
public final class StaticInterfaceRepository implements InterfaceRepository {

  private final List<IdlGeneratedTypeDescriptor> descriptors;
  private final Map<RepositoryId, IdlGeneratedTypeDescriptor> byRepositoryId;
  private final Map<String, IdlGeneratedTypeDescriptor> byIdlScopedName;
  private final Map<String, IdlGeneratedTypeDescriptor> byJavaName;

  private StaticInterfaceRepository(
      List<IdlGeneratedTypeDescriptor> descriptors,
      Map<RepositoryId, IdlGeneratedTypeDescriptor> byRepositoryId,
      Map<String, IdlGeneratedTypeDescriptor> byIdlScopedName,
      Map<String, IdlGeneratedTypeDescriptor> byJavaName) {
    this.descriptors = descriptors;
    this.byRepositoryId = byRepositoryId;
    this.byIdlScopedName = byIdlScopedName;
    this.byJavaName = byJavaName;
  }

  /** Creates a static repository from descriptors in deterministic encounter order. */
  public static StaticInterfaceRepository of(List<IdlGeneratedTypeDescriptor> descriptors) {
    List<IdlGeneratedTypeDescriptor> ordered =
        List.copyOf(Objects.requireNonNull(descriptors, "descriptors"));
    Map<RepositoryId, IdlGeneratedTypeDescriptor> byRepositoryId = new LinkedHashMap<>();
    Map<String, IdlGeneratedTypeDescriptor> byIdlScopedName = new LinkedHashMap<>();
    Map<String, IdlGeneratedTypeDescriptor> byJavaName = new LinkedHashMap<>();
    for (IdlGeneratedTypeDescriptor descriptor : ordered) {
      validateDescriptor(Objects.requireNonNull(descriptor, "descriptor"));
      putUnique(byRepositoryId, descriptor.repositoryId(), descriptor, "repository ID");
      putUnique(byIdlScopedName, descriptor.idlScopedName(), descriptor, "IDL scoped name");
      putUnique(byJavaName, descriptor.javaName(), descriptor, "Java name");
    }
    return new StaticInterfaceRepository(
        ordered, Map.copyOf(byRepositoryId), Map.copyOf(byIdlScopedName), Map.copyOf(byJavaName));
  }

  @Override
  public List<IdlGeneratedTypeDescriptor> descriptors() {
    return List.copyOf(descriptors);
  }

  @Override
  public Optional<IdlGeneratedTypeDescriptor> findByRepositoryId(RepositoryId repositoryId) {
    return Optional.ofNullable(
        byRepositoryId.get(Objects.requireNonNull(repositoryId, "repositoryId")));
  }

  @Override
  public Optional<IdlGeneratedTypeDescriptor> findByIdlScopedName(String idlScopedName) {
    return Optional.ofNullable(
        byIdlScopedName.get(requireNonBlank(idlScopedName, "idlScopedName")));
  }

  @Override
  public Optional<IdlGeneratedTypeDescriptor> findByJavaName(String javaName) {
    return Optional.ofNullable(byJavaName.get(requireNonBlank(javaName, "javaName")));
  }

  @Override
  public IdlGeneratedTypeDescriptor requireByRepositoryId(RepositoryId repositoryId) {
    return findByRepositoryId(repositoryId)
        .orElseThrow(
            () ->
                new InterfaceRepositoryException(
                    InterfaceRepositoryDiagnosticCodes.MISSING_DESCRIPTOR,
                    "missing descriptor for repository ID: " + repositoryId));
  }

  @Override
  public IdlOperationDescriptor requireOperation(RepositoryId repositoryId, String operationName) {
    IdlGeneratedTypeDescriptor descriptor = requireByRepositoryId(repositoryId);
    String checkedName = requireNonBlank(operationName, "operationName");
    return descriptor.operations().stream()
        .filter(operation -> operation.name().equals(checkedName))
        .findFirst()
        .orElseThrow(
            () ->
                new InterfaceRepositoryException(
                    InterfaceRepositoryDiagnosticCodes.MISSING_DESCRIPTOR,
                    "missing operation " + checkedName + " for repository ID: " + repositoryId));
  }

  @Override
  public IdlTypeCode typeCode(IdlGeneratedTypeDescriptor descriptor) {
    IdlGeneratedTypeDescriptor checked = Objects.requireNonNull(descriptor, "descriptor");
    IdlGeneratedTypeDescriptor registered = requireByRepositoryId(checked.repositoryId());
    if (!registered.equals(checked)) {
      throw new InterfaceRepositoryException(
          InterfaceRepositoryDiagnosticCodes.INVALID_REFERENCE,
          "descriptor does not match registered metadata: " + checked.repositoryId());
    }
    return IdlTypeCode.fromDescriptor(registered, this::resolveTypeCode);
  }

  private IdlTypeCode resolveTypeCode(IdlTypeReference reference) {
    Objects.requireNonNull(reference, "reference");
    if (reference.kind() == IdlTypeKind.PRIMITIVE || reference.kind() == IdlTypeKind.VOID) {
      return IdlTypeCode.fromTypeReference(reference);
    }
    RepositoryId repositoryId =
        reference
            .repositoryId()
            .orElseThrow(
                () ->
                    new InterfaceRepositoryException(
                        InterfaceRepositoryDiagnosticCodes.INVALID_REFERENCE,
                        "generated reference lacks repository ID: " + reference.idlName()));
    return typeCode(requireByRepositoryId(repositoryId));
  }

  private static void validateDescriptor(IdlGeneratedTypeDescriptor descriptor) {
    if (descriptor.kind() == IdlTypeKind.PRIMITIVE || descriptor.kind() == IdlTypeKind.VOID) {
      throw new InterfaceRepositoryException(
          InterfaceRepositoryDiagnosticCodes.UNSUPPORTED_DESCRIPTOR_KIND,
          "unsupported descriptor kind: " + descriptor.kind());
    }
  }

  private static <K> void putUnique(
      Map<K, IdlGeneratedTypeDescriptor> values,
      K key,
      IdlGeneratedTypeDescriptor descriptor,
      String label) {
    IdlGeneratedTypeDescriptor existing = values.putIfAbsent(key, descriptor);
    if (existing != null) {
      throw new InterfaceRepositoryException(
          InterfaceRepositoryDiagnosticCodes.DUPLICATE_DESCRIPTOR,
          "duplicate descriptor " + label + ": " + key);
    }
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
