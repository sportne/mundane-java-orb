package io.github.mundanej.mjo.typecode;

import io.github.mundanej.mjo.cdr.CdrReader;
import io.github.mundanej.mjo.cdr.CdrWriter;
import io.github.mundanej.mjo.repositoryid.RepositoryId;
import java.util.List;
import java.util.Optional;

/** Native Image smoke check for descriptor metadata and compile-only codec failures. */
public final class TypecodeDescriptorNativeSmoke {

  private TypecodeDescriptorNativeSmoke() {}

  /** Runs the smoke check. */
  public static void main(String[] args) {
    RepositoryId repositoryId = RepositoryId.parse("IDL:hello/Greeter:1.0");
    IdlTypeReference stringType =
        new IdlTypeReference(IdlTypeKind.PRIMITIVE, "string", "java.lang.String", Optional.empty());
    IdlOperationDescriptor greet =
        new IdlOperationDescriptor(
            "greet",
            stringType,
            List.of(new IdlParameterDescriptor("name", IdlParameterMode.IN, stringType)),
            List.of());
    IdlGeneratedTypeDescriptor descriptor =
        new IdlGeneratedTypeDescriptor(
            IdlTypeKind.INTERFACE,
            "::hello::Greeter",
            "hello.Greeter",
            repositoryId,
            List.of(),
            List.of(),
            List.of(greet));

    require("IDL:hello/Greeter:1.0".equals(descriptor.repositoryId().value()));
    require(
        descriptor.operations().getFirst().parameters().getFirst().mode() == IdlParameterMode.IN);
    require(IdlTypeCode.fromDescriptor(descriptor).kind() == IdlTypeCodeKind.INTERFACE);
    require(
        IdlTypeCode.STRING.equals(
            IdlTypeCode.sequenceOf(IdlTypeCode.STRING, "sequence<string>", "java.util.List")
                .elementType()
                .orElseThrow()));

    UnsupportedIdlCodec<Object> codec =
        new UnsupportedIdlCodec<>(
            "CDR codec is compile-only in G6-220; CDR string support is deferred to G6-320.");
    requireUnsupported(() -> codec.read(CdrReader.bigEndian(new byte[0])));
    requireUnsupported(() -> codec.write(CdrWriter.bigEndian(), new Object()));
  }

  private static void require(boolean condition) {
    if (!condition) {
      throw new IllegalStateException("native descriptor smoke check failed");
    }
  }

  private static void requireUnsupported(Runnable runnable) {
    try {
      runnable.run();
    } catch (UnsupportedOperationException expected) {
      if (expected.getMessage().contains("G6-320")) {
        return;
      }
      throw expected;
    }
    throw new IllegalStateException("expected unsupported codec failure");
  }
}
