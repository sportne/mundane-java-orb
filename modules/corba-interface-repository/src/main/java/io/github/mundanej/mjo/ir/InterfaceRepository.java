package io.github.mundanej.mjo.ir;

import io.github.mundanej.mjo.repositoryid.RepositoryId;
import io.github.mundanej.mjo.typecode.IdlGeneratedTypeDescriptor;
import io.github.mundanej.mjo.typecode.IdlOperationDescriptor;
import io.github.mundanej.mjo.typecode.IdlTypeCode;
import java.util.List;
import java.util.Optional;

/** Local static Interface Repository lookup surface over generated descriptors. */
public interface InterfaceRepository {

  /** Returns descriptors in deterministic repository encounter order. */
  List<IdlGeneratedTypeDescriptor> descriptors();

  /** Finds a descriptor by repository ID. */
  Optional<IdlGeneratedTypeDescriptor> findByRepositoryId(RepositoryId repositoryId);

  /** Finds a descriptor by absolute IDL scoped name. */
  Optional<IdlGeneratedTypeDescriptor> findByIdlScopedName(String idlScopedName);

  /** Finds a descriptor by mapped Java qualified name. */
  Optional<IdlGeneratedTypeDescriptor> findByJavaName(String javaName);

  /** Returns a descriptor by repository ID or fails deterministically. */
  IdlGeneratedTypeDescriptor requireByRepositoryId(RepositoryId repositoryId);

  /** Returns an operation descriptor by declaring type repository ID and IDL operation name. */
  IdlOperationDescriptor requireOperation(RepositoryId repositoryId, String operationName);

  /** Builds a local TypeCode for one registered descriptor. */
  IdlTypeCode typeCode(IdlGeneratedTypeDescriptor descriptor);
}
