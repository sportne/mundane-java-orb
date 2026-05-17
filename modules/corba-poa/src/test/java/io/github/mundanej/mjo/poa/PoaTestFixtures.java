package io.github.mundanej.mjo.poa;

import io.github.mundanej.mjo.orb.LocalOrb;
import io.github.mundanej.mjo.repositoryid.RepositoryId;
import io.github.mundanej.mjo.typecode.IdlGeneratedTypeDescriptor;
import io.github.mundanej.mjo.typecode.IdlOperationDescriptor;
import io.github.mundanej.mjo.typecode.IdlParameterDescriptor;
import io.github.mundanej.mjo.typecode.IdlParameterMode;
import io.github.mundanej.mjo.typecode.IdlTypeKind;
import io.github.mundanej.mjo.typecode.IdlTypeReference;
import java.util.List;
import java.util.Optional;

final class PoaTestFixtures {

  static final IdlTypeReference STRING_TYPE =
      new IdlTypeReference(IdlTypeKind.PRIMITIVE, "string", "java.lang.String", Optional.empty());
  static final IdlOperationDescriptor GREET =
      new IdlOperationDescriptor(
          "greet",
          STRING_TYPE,
          List.of(new IdlParameterDescriptor("name", IdlParameterMode.IN, STRING_TYPE)),
          List.of());
  static final IdlGeneratedTypeDescriptor GREETER_DESCRIPTOR =
      new IdlGeneratedTypeDescriptor(
          IdlTypeKind.INTERFACE,
          "::hello::Greeter",
          Greeter.class.getName(),
          RepositoryId.parse("IDL:hello/Greeter:1.0"),
          List.of(),
          List.of(),
          List.of(GREET));
  static final PoaServantDispatcher<GreeterServant> GREETER_DISPATCHER =
      (target, request) -> target.greet((String) request.arguments().get(0));
  static final PoaServantDispatcher<Object> OBJECT_GREETER_DISPATCHER =
      (target, request) -> ((Greeter) target).greet((String) request.arguments().get(0));

  private PoaTestFixtures() {}

  static String invoke(LocalOrb orb, io.github.mundanej.mjo.orb.LocalObjectReference<?> reference) {
    return (String) orb.invoke(reference, GREET, List.of("Ada"));
  }

  interface Greeter {

    String greet(String name);
  }

  static final class GreeterServant implements Greeter {

    private final String prefix;

    GreeterServant() {
      this("Hello ");
    }

    GreeterServant(String prefix) {
      this.prefix = prefix;
    }

    @Override
    public String greet(String name) {
      return prefix + name;
    }
  }
}
