package io.github.mundanej.mjo.modern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mundanej.mjo.repositoryid.RepositoryId;
import io.github.mundanej.mjo.typecode.IdlGeneratedTypeDescriptor;
import io.github.mundanej.mjo.typecode.IdlOperationDescriptor;
import io.github.mundanej.mjo.typecode.IdlParameterDescriptor;
import io.github.mundanej.mjo.typecode.IdlParameterMode;
import io.github.mundanej.mjo.typecode.IdlTypeKind;
import io.github.mundanej.mjo.typecode.IdlTypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit tests for generated-code-facing local invocation contracts. */
@Tag("unit")
final class LocalInvocationRequestTest {

  private static final IdlTypeReference STRING_TYPE =
      new IdlTypeReference(IdlTypeKind.PRIMITIVE, "string", "java.lang.String", Optional.empty());
  private static final IdlOperationDescriptor GREET =
      new IdlOperationDescriptor(
          "greet",
          STRING_TYPE,
          List.of(new IdlParameterDescriptor("name", IdlParameterMode.IN, STRING_TYPE)),
          List.of());
  private static final IdlGeneratedTypeDescriptor GREETER =
      new IdlGeneratedTypeDescriptor(
          IdlTypeKind.INTERFACE,
          "::hello::Greeter",
          "modern.hello.Greeter",
          RepositoryId.parse("IDL:hello/Greeter:1.0"),
          List.of(),
          List.of(),
          List.of(GREET));

  @Test
  void defensivelyCopiesInvocationArguments() {
    List<Object> arguments = new ArrayList<>();
    arguments.add("Ada");
    LocalInvocationRequest request = new LocalInvocationRequest(GREETER, GREET, arguments);
    arguments.set(0, "Grace");

    assertEquals(GREETER, request.targetDescriptor());
    assertEquals(GREET, request.operation());
    assertEquals(List.of("Ada"), request.arguments());
    assertThrows(UnsupportedOperationException.class, () -> request.arguments().add("extra"));
  }

  @Test
  void emptyAndNullArgumentValuesHaveDeterministicRecordSemantics() {
    LocalInvocationRequest empty = new LocalInvocationRequest(GREETER, GREET, List.of());
    LocalInvocationRequest withNull =
        new LocalInvocationRequest(GREETER, GREET, java.util.Arrays.asList("Ada", null));

    assertEquals(new LocalInvocationRequest(GREETER, GREET, List.of()), empty);
    assertEquals(List.of(), empty.arguments());
    assertEquals(java.util.Arrays.asList("Ada", null), withNull.arguments());
  }

  @Test
  void rejectsMissingRequestFields() {
    assertThrows(
        NullPointerException.class, () -> new LocalInvocationRequest(null, GREET, List.of()));
    assertThrows(
        NullPointerException.class, () -> new LocalInvocationRequest(GREETER, null, List.of()));
    assertThrows(
        NullPointerException.class, () -> new LocalInvocationRequest(GREETER, GREET, null));
  }

  @Test
  void dispatcherApiSupportsGeneratedSkeletonStyleImplementation() throws Exception {
    LocalInvocationDispatcher dispatcher =
        request -> "Hello " + request.arguments().get(0).toString();

    assertEquals(
        "Hello Ada", dispatcher.invoke(new LocalInvocationRequest(GREETER, GREET, List.of("Ada"))));
  }

  @Test
  void dispatcherApiSupportsCheckedUserExceptions() {
    LocalInvocationDispatcher dispatcher =
        request -> {
          throw new DeclaredUserException("declared");
        };

    assertThrows(
        DeclaredUserException.class,
        () -> dispatcher.invoke(new LocalInvocationRequest(GREETER, GREET, List.of("Ada"))));
  }

  private static final class DeclaredUserException extends Exception {

    private static final long serialVersionUID = 1L;

    private DeclaredUserException(String message) {
      super(message);
    }
  }
}
