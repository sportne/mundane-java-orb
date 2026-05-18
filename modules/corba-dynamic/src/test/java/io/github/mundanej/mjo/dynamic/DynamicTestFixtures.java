package io.github.mundanej.mjo.dynamic;

import io.github.mundanej.mjo.any.AnyCodecs;
import io.github.mundanej.mjo.any.AnyValue;
import io.github.mundanej.mjo.repositoryid.RepositoryId;
import io.github.mundanej.mjo.typecode.IdlFieldDescriptor;
import io.github.mundanej.mjo.typecode.IdlGeneratedTypeDescriptor;
import io.github.mundanej.mjo.typecode.IdlOperationDescriptor;
import io.github.mundanej.mjo.typecode.IdlParameterDescriptor;
import io.github.mundanej.mjo.typecode.IdlParameterMode;
import io.github.mundanej.mjo.typecode.IdlTypeCode;
import io.github.mundanej.mjo.typecode.IdlTypeKind;
import io.github.mundanej.mjo.typecode.IdlTypeReference;
import java.util.List;
import java.util.Optional;

/** Shared descriptor fixtures for dynamic module tests. */
final class DynamicTestFixtures {

  static final IdlTypeReference LONG_REFERENCE =
      new IdlTypeReference(IdlTypeKind.PRIMITIVE, "long", "int", Optional.empty());
  static final IdlTypeReference STRING_REFERENCE =
      new IdlTypeReference(IdlTypeKind.PRIMITIVE, "string", "java.lang.String", Optional.empty());
  static final IdlTypeReference VOID_REFERENCE =
      new IdlTypeReference(IdlTypeKind.PRIMITIVE, "void", "void", Optional.empty());
  static final IdlTypeReference PROBLEM_REFERENCE =
      new IdlTypeReference(
          IdlTypeKind.EXCEPTION,
          "::demo::Problem",
          DemoProblem.class.getName(),
          Optional.of(RepositoryId.parse("IDL:demo/Problem:1.0")));

  static final IdlOperationDescriptor ADD_OPERATION =
      new IdlOperationDescriptor(
          "add",
          LONG_REFERENCE,
          List.of(
              new IdlParameterDescriptor("left", IdlParameterMode.IN, LONG_REFERENCE),
              new IdlParameterDescriptor("right", IdlParameterMode.IN, LONG_REFERENCE)),
          List.of(PROBLEM_REFERENCE));

  static final IdlOperationDescriptor PING_OPERATION =
      new IdlOperationDescriptor("ping", VOID_REFERENCE, List.of(), List.of());

  static final IdlGeneratedTypeDescriptor SERVICE_DESCRIPTOR =
      new IdlGeneratedTypeDescriptor(
          IdlTypeKind.INTERFACE,
          "::demo::Calculator",
          Calculator.class.getName(),
          RepositoryId.parse("IDL:demo/Calculator:1.0"),
          List.of(),
          List.of(),
          List.of(ADD_OPERATION, PING_OPERATION));

  private DynamicTestFixtures() {}

  static DynamicOperationCodec addCodec() {
    return DynamicOperationCodec.valueReturn(
        ADD_OPERATION,
        AnyCodecs.longCodec(),
        List.of(AnyCodecs.longCodec(), AnyCodecs.longCodec()));
  }

  static DynamicOperationCodec pingCodec() {
    return DynamicOperationCodec.voidReturn(PING_OPERATION, List.of());
  }

  static AnyValue<Integer> longAny(int value) {
    return new AnyValue<>(IdlTypeCode.LONG, value);
  }

  static IdlTypeCode pointType() {
    return IdlTypeCode.fromDescriptor(
        new IdlGeneratedTypeDescriptor(
            IdlTypeKind.STRUCT,
            "::demo::Point",
            "demo.Point",
            RepositoryId.parse("IDL:demo/Point:1.0"),
            List.of(
                new IdlFieldDescriptor("x", LONG_REFERENCE),
                new IdlFieldDescriptor("label", STRING_REFERENCE)),
            List.of(),
            List.of()));
  }

  static IdlTypeCode problemType() {
    return IdlTypeCode.fromDescriptor(
        new IdlGeneratedTypeDescriptor(
            IdlTypeKind.EXCEPTION,
            "::demo::Problem",
            DemoProblem.class.getName(),
            RepositoryId.parse("IDL:demo/Problem:1.0"),
            List.of(new IdlFieldDescriptor("message", STRING_REFERENCE)),
            List.of(),
            List.of()));
  }

  static IdlTypeCode colorType() {
    return IdlTypeCode.fromDescriptor(
        new IdlGeneratedTypeDescriptor(
            IdlTypeKind.ENUM,
            "::demo::Color",
            "demo.Color",
            RepositoryId.parse("IDL:demo/Color:1.0"),
            List.of(),
            List.of("RED", "GREEN"),
            List.of()));
  }

  static DynamicOperationCodec unsupportedOutOperation() {
    IdlOperationDescriptor operation =
        new IdlOperationDescriptor(
            "out",
            VOID_REFERENCE,
            List.of(new IdlParameterDescriptor("value", IdlParameterMode.OUT, LONG_REFERENCE)),
            List.of());
    return DynamicOperationCodec.voidReturn(operation, List.of(AnyCodecs.longCodec()));
  }

  interface Calculator {}

  static final class DemoProblem extends Exception {
    private static final long serialVersionUID = 1L;
  }
}
