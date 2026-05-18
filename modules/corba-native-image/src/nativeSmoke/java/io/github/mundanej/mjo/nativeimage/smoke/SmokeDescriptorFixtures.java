package io.github.mundanej.mjo.nativeimage.smoke;

import io.github.mundanej.mjo.repositoryid.RepositoryId;
import io.github.mundanej.mjo.typecode.IdlGeneratedTypeDescriptor;
import io.github.mundanej.mjo.typecode.IdlOperationDescriptor;
import io.github.mundanej.mjo.typecode.IdlParameterDescriptor;
import io.github.mundanej.mjo.typecode.IdlParameterMode;
import io.github.mundanej.mjo.typecode.IdlTypeKind;
import io.github.mundanej.mjo.typecode.IdlTypeReference;
import java.util.List;
import java.util.Optional;

final class SmokeDescriptorFixtures {

  static final IdlTypeReference STRING =
      new IdlTypeReference(IdlTypeKind.PRIMITIVE, "string", "java.lang.String", Optional.empty());

  static final IdlOperationDescriptor GREET =
      new IdlOperationDescriptor(
          "greet",
          STRING,
          List.of(new IdlParameterDescriptor("name", IdlParameterMode.IN, STRING)),
          List.of());

  static final IdlGeneratedTypeDescriptor GREETER =
      new IdlGeneratedTypeDescriptor(
          IdlTypeKind.INTERFACE,
          "::nativeimage::Greeter",
          Greeter.class.getName(),
          RepositoryId.parse("IDL:nativeimage/Greeter:1.0"),
          List.of(),
          List.of(),
          List.of(GREET));

  private SmokeDescriptorFixtures() {}

  interface Greeter {}
}
