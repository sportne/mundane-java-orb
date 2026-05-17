package io.github.mundanej.mjo.orb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjo.modern.LocalInvocationDispatcher;
import io.github.mundanej.mjo.modern.LocalInvocationRequest;
import io.github.mundanej.mjo.repositoryid.RepositoryId;
import io.github.mundanej.mjo.typecode.IdlGeneratedTypeDescriptor;
import io.github.mundanej.mjo.typecode.IdlOperationDescriptor;
import io.github.mundanej.mjo.typecode.IdlParameterDescriptor;
import io.github.mundanej.mjo.typecode.IdlParameterMode;
import io.github.mundanej.mjo.typecode.IdlTypeKind;
import io.github.mundanej.mjo.typecode.IdlTypeReference;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit and integration tests for local ORB object reference invocation. */
@Tag("unit")
final class LocalOrbTest {

  private static final IdlTypeReference STRING_TYPE =
      new IdlTypeReference(IdlTypeKind.PRIMITIVE, "string", "java.lang.String", Optional.empty());
  private static final IdlOperationDescriptor GREET =
      new IdlOperationDescriptor(
          "greet",
          STRING_TYPE,
          List.of(new IdlParameterDescriptor("name", IdlParameterMode.IN, STRING_TYPE)),
          List.of());
  private static final IdlOperationDescriptor FAREWELL =
      new IdlOperationDescriptor("farewell", STRING_TYPE, List.of(), List.of());
  private static final IdlGeneratedTypeDescriptor GREETER_DESCRIPTOR =
      new IdlGeneratedTypeDescriptor(
          IdlTypeKind.INTERFACE,
          "::hello::Greeter",
          "modern.hello.Greeter",
          RepositoryId.parse("IDL:hello/Greeter:1.0"),
          List.of(),
          List.of(),
          List.of(GREET));

  @Test
  void generatedStyleClientCallsLocalGeneratedStyleDispatcher() {
    LocalOrb orb = LocalOrb.create();
    LocalObjectReference<Greeter> reference =
        orb.bind(Greeter.class, GREETER_DESCRIPTOR, new GreeterDispatcher(new GreeterServant()));
    GreeterClient client = new GreeterClient(orb, reference);

    assertEquals("Hello Ada", client.greet("Ada"));
    assertEquals(Greeter.class, reference.javaType());
    assertEquals(GREETER_DESCRIPTOR, reference.descriptor());
    assertEquals(
        "LocalObjectReference[objectId=local-1, javaType=" + Greeter.class.getName() + "]",
        reference.toString());
  }

  @Test
  void objectReferencesUseStableLocalIdentity() {
    LocalOrb firstOrb = LocalOrb.create();
    LocalObjectReference<Greeter> first =
        firstOrb.bind(
            Greeter.class, GREETER_DESCRIPTOR, new GreeterDispatcher(new GreeterServant()));
    LocalObjectReference<Greeter> second =
        firstOrb.bind(
            Greeter.class, GREETER_DESCRIPTOR, new GreeterDispatcher(new GreeterServant()));
    LocalObjectReference<Greeter> otherOrbFirst =
        LocalOrb.create()
            .bind(Greeter.class, GREETER_DESCRIPTOR, new GreeterDispatcher(new GreeterServant()));

    assertEquals("local-1", first.objectId());
    assertEquals("local-2", second.objectId());
    assertEquals(first, first);
    assertEquals(first.hashCode(), first.hashCode());
    assertNotEquals(first, second);
    assertNotEquals(first, otherOrbFirst);
  }

  @Test
  void staleReferenceFailsDeterministically() {
    LocalObjectReference<Greeter> otherOrbReference =
        LocalOrb.create()
            .bind(Greeter.class, GREETER_DESCRIPTOR, new GreeterDispatcher(new GreeterServant()));
    LocalOrb orb = LocalOrb.create();

    LocalOrbException exception =
        assertThrows(
            LocalOrbException.class, () -> orb.invoke(otherOrbReference, GREET, List.of("Ada")));

    assertEquals("Local object reference belongs to a different local ORB", exception.getMessage());
  }

  @Test
  void operationAndArgumentValidationHappensBeforeDispatch() {
    LocalOrb orb = LocalOrb.create();
    CountingDispatcher dispatcher = new CountingDispatcher();
    LocalObjectReference<Greeter> reference =
        orb.bind(Greeter.class, GREETER_DESCRIPTOR, dispatcher);

    assertThrows(LocalOrbException.class, () -> orb.invoke(reference, FAREWELL, List.of()));
    assertThrows(LocalOrbException.class, () -> orb.invoke(reference, GREET, List.of()));
    assertEquals(0, dispatcher.count());
  }

  @Test
  void shutdownBlocksBindAndInvokeAndIsIdempotent() {
    LocalOrb orb = LocalOrb.create();
    LocalObjectReference<Greeter> reference =
        orb.bind(Greeter.class, GREETER_DESCRIPTOR, new GreeterDispatcher(new GreeterServant()));

    orb.shutdown();
    orb.shutdown();

    assertTrue(orb.isShutdown());
    assertThrows(
        LocalOrbException.class,
        () ->
            orb.bind(
                Greeter.class, GREETER_DESCRIPTOR, new GreeterDispatcher(new GreeterServant())));
    assertThrows(LocalOrbException.class, () -> orb.invoke(reference, GREET, List.of("Ada")));
  }

  @Test
  void localInvocationDoesNotRequireNetworkTransportClasses() {
    LocalOrb orb = LocalOrb.create();
    LocalObjectReference<Greeter> reference =
        orb.bind(
            Greeter.class, GREETER_DESCRIPTOR, request -> "local:" + request.arguments().get(0));

    assertEquals("local:Ada", orb.invoke(reference, GREET, List.of("Ada")));
  }

  private interface Greeter {

    String greet(String name);
  }

  private static final class GreeterServant implements Greeter {

    @Override
    public String greet(String name) {
      return "Hello " + name;
    }
  }

  private static final class GreeterClient implements Greeter {

    private final LocalOrb orb;
    private final LocalObjectReference<Greeter> reference;

    private GreeterClient(LocalOrb orb, LocalObjectReference<Greeter> reference) {
      this.orb = orb;
      this.reference = reference;
    }

    @Override
    public String greet(String name) {
      return (String) orb.invoke(reference, GREET, List.of(name));
    }
  }

  private static final class GreeterDispatcher implements LocalInvocationDispatcher {

    private final Greeter servant;

    private GreeterDispatcher(Greeter servant) {
      this.servant = servant;
    }

    @Override
    public Object invoke(LocalInvocationRequest request) {
      return servant.greet((String) request.arguments().get(0));
    }
  }

  private static final class CountingDispatcher implements LocalInvocationDispatcher {

    private int count;

    @Override
    public Object invoke(LocalInvocationRequest request) {
      count++;
      return "unused";
    }

    private int count() {
      return count;
    }
  }
}
