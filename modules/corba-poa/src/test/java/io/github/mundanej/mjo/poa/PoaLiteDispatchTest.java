package io.github.mundanej.mjo.poa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjo.orb.LocalObjectReference;
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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.omg.CORBA.BAD_INV_ORDER;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.OBJECT_NOT_EXIST;

/** Unit and integration tests for POA-lite servant dispatch. */
@Tag("unit")
final class PoaLiteDispatchTest {

  private static final IdlTypeReference STRING_TYPE =
      new IdlTypeReference(IdlTypeKind.PRIMITIVE, "string", "java.lang.String", Optional.empty());
  private static final IdlOperationDescriptor GREET =
      new IdlOperationDescriptor(
          "greet",
          STRING_TYPE,
          List.of(new IdlParameterDescriptor("name", IdlParameterMode.IN, STRING_TYPE)),
          List.of());
  private static final IdlGeneratedTypeDescriptor GREETER_DESCRIPTOR =
      new IdlGeneratedTypeDescriptor(
          IdlTypeKind.INTERFACE,
          "::hello::Greeter",
          Greeter.class.getName(),
          RepositoryId.parse("IDL:hello/Greeter:1.0"),
          List.of(),
          List.of(),
          List.of(GREET));

  @Test
  void generatedStyleClientInvokesActivatedServantThroughPoaLite() {
    LocalOrb orb = LocalOrb.create();
    PoaLite poa = PoaLite.createRoot(orb);
    GreeterServant servant = new GreeterServant();
    LocalObjectReference<Greeter> reference =
        poa.activateServant(
            Greeter.class,
            GREETER_DESCRIPTOR,
            servant,
            (target, request) -> target.greet((String) request.arguments().get(0)));
    Greeter client = new GreeterClient(orb, reference);

    assertEquals("Hello Ada", client.greet("Ada"));
    assertEquals("local-1", reference.objectId());
    assertEquals(1, poa.activeObjectCount());
    assertFalse(poa.isShutdown());
  }

  @Test
  void distinctActivationsReceiveDistinctStableObjectIds() {
    LocalOrb orb = LocalOrb.create();
    PoaLite poa = PoaLite.createRoot(orb);

    LocalObjectReference<Greeter> first =
        poa.activateServant(
            Greeter.class,
            GREETER_DESCRIPTOR,
            new GreeterServant(),
            (target, request) -> target.greet((String) request.arguments().get(0)));
    LocalObjectReference<Greeter> second =
        poa.activateServant(
            Greeter.class,
            GREETER_DESCRIPTOR,
            new GreeterServant(),
            (target, request) -> target.greet((String) request.arguments().get(0)));

    assertEquals("local-1", first.objectId());
    assertEquals("local-2", second.objectId());
    assertEquals(2, poa.activeObjectCount());
    assertEquals("Hello Grace", orb.invoke(second, GREET, List.of("Grace")));
  }

  @Test
  void uniqueIdPolicyRejectsDuplicateServantActivationBeforeBindingNewId() {
    LocalOrb orb = LocalOrb.create();
    PoaLite poa = PoaLite.createRoot(orb);
    GreeterServant servant = new GreeterServant();
    PoaServantDispatcher<GreeterServant> dispatcher =
        (target, request) -> target.greet((String) request.arguments().get(0));

    LocalObjectReference<Greeter> first =
        poa.activateServant(Greeter.class, GREETER_DESCRIPTOR, servant, dispatcher);
    BAD_PARAM exception =
        assertThrows(
            BAD_PARAM.class,
            () -> poa.activateServant(Greeter.class, GREETER_DESCRIPTOR, servant, dispatcher));
    LocalObjectReference<Greeter> second =
        poa.activateServant(Greeter.class, GREETER_DESCRIPTOR, new GreeterServant(), dispatcher);

    assertEquals(CompletionStatus.COMPLETED_NO, exception.completed);
    assertEquals("local-1", first.objectId());
    assertEquals("local-2", second.objectId());
    assertEquals(2, poa.activeObjectCount());
  }

  @Test
  void deactivationRemovesActiveObjectMapEntryAndBlocksLaterInvocation() {
    LocalOrb orb = LocalOrb.create();
    PoaLite poa = PoaLite.createRoot(orb);
    LocalObjectReference<Greeter> reference =
        poa.activateServant(
            Greeter.class,
            GREETER_DESCRIPTOR,
            new GreeterServant(),
            (target, request) -> target.greet((String) request.arguments().get(0)));

    poa.deactivateObject(reference.objectId());

    assertEquals(0, poa.activeObjectCount());
    OBJECT_NOT_EXIST invokeFailure =
        assertThrows(OBJECT_NOT_EXIST.class, () -> orb.invoke(reference, GREET, List.of("Ada")));
    OBJECT_NOT_EXIST unknownFailure =
        assertThrows(OBJECT_NOT_EXIST.class, () -> poa.deactivateObject("missing"));

    assertEquals(CompletionStatus.COMPLETED_NO, invokeFailure.completed);
    assertEquals(CompletionStatus.COMPLETED_NO, unknownFailure.completed);
  }

  @Test
  void shutdownIsIdempotentAndBlocksActivationAndInvocation() {
    LocalOrb orb = LocalOrb.create();
    PoaLite poa = PoaLite.createRoot(orb);
    LocalObjectReference<Greeter> reference =
        poa.activateServant(
            Greeter.class,
            GREETER_DESCRIPTOR,
            new GreeterServant(),
            (target, request) -> target.greet((String) request.arguments().get(0)));

    poa.shutdown();
    poa.shutdown();

    assertTrue(poa.isShutdown());
    assertEquals(0, poa.activeObjectCount());
    BAD_INV_ORDER activationFailure =
        assertThrows(
            BAD_INV_ORDER.class,
            () ->
                poa.activateServant(
                    Greeter.class,
                    GREETER_DESCRIPTOR,
                    new GreeterServant(),
                    (target, request) -> target.greet((String) request.arguments().get(0))));
    BAD_INV_ORDER invocationFailure =
        assertThrows(BAD_INV_ORDER.class, () -> orb.invoke(reference, GREET, List.of("Ada")));

    assertEquals(CompletionStatus.COMPLETED_NO, activationFailure.completed);
    assertEquals(CompletionStatus.COMPLETED_NO, invocationFailure.completed);
  }

  @Test
  void nullInputsFailAsBadParamBeforeActivation() {
    LocalOrb orb = LocalOrb.create();
    PoaLite poa = PoaLite.createRoot(orb);
    GreeterServant servant = new GreeterServant();
    PoaServantDispatcher<GreeterServant> dispatcher =
        (target, request) -> target.greet((String) request.arguments().get(0));

    assertBadParam(() -> PoaLite.createRoot(null));
    assertBadParam(() -> PoaLite.createRoot(orb, null));
    assertBadParam(() -> poa.activateServant(null, GREETER_DESCRIPTOR, servant, dispatcher));
    assertBadParam(() -> poa.activateServant(Greeter.class, null, servant, dispatcher));
    assertBadParam(() -> poa.activateServant(Greeter.class, GREETER_DESCRIPTOR, null, dispatcher));
    assertBadParam(() -> poa.activateServant(Greeter.class, GREETER_DESCRIPTOR, servant, null));
    assertBadParam(() -> poa.deactivateObject(null));

    assertEquals(0, poa.activeObjectCount());
  }

  @Test
  void dispatcherReceivesOriginalServantAndGeneratedRequest() {
    LocalOrb orb = LocalOrb.create();
    PoaLite poa = PoaLite.createRoot(orb);
    GreeterServant servant = new GreeterServant();

    LocalObjectReference<Greeter> reference =
        poa.activateServant(
            Greeter.class,
            GREETER_DESCRIPTOR,
            servant,
            (target, request) -> {
              assertSame(servant, target);
              assertEquals(GREETER_DESCRIPTOR, request.targetDescriptor());
              assertEquals(GREET, request.operation());
              return target.greet((String) request.arguments().get(0));
            });

    assertEquals("Hello Ada", orb.invoke(reference, GREET, List.of("Ada")));
  }

  private static void assertBadParam(ThrowingAction action) {
    BAD_PARAM exception = assertThrows(BAD_PARAM.class, action::run);

    assertEquals(CompletionStatus.COMPLETED_NO, exception.completed);
  }

  @FunctionalInterface
  private interface ThrowingAction {

    void run();
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
}
