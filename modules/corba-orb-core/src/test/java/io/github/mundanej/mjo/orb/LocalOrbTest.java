package io.github.mundanej.mjo.orb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
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
import org.omg.CORBA.BAD_INV_ORDER;
import org.omg.CORBA.BAD_OPERATION;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.CORBA.UNKNOWN;

/** Unit and integration tests for local ORB object reference invocation. */
@Tag("unit")
final class LocalOrbTest {

  private static final IdlTypeReference STRING_TYPE =
      new IdlTypeReference(IdlTypeKind.PRIMITIVE, "string", "java.lang.String", Optional.empty());
  private static final IdlTypeReference BAD_NAME_TYPE =
      new IdlTypeReference(
          IdlTypeKind.EXCEPTION,
          "::hello::BadName",
          BadName.class.getName(),
          Optional.of(RepositoryId.parse("IDL:hello/BadName:1.0")));
  private static final IdlOperationDescriptor GREET =
      new IdlOperationDescriptor(
          "greet",
          STRING_TYPE,
          List.of(new IdlParameterDescriptor("name", IdlParameterMode.IN, STRING_TYPE)),
          List.of());
  private static final IdlOperationDescriptor RISKY_GREET =
      new IdlOperationDescriptor(
          "greet",
          STRING_TYPE,
          List.of(new IdlParameterDescriptor("name", IdlParameterMode.IN, STRING_TYPE)),
          List.of(BAD_NAME_TYPE));
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
  private static final IdlGeneratedTypeDescriptor RISKY_GREETER_DESCRIPTOR =
      new IdlGeneratedTypeDescriptor(
          IdlTypeKind.INTERFACE,
          "::hello::RiskyGreeter",
          "modern.hello.RiskyGreeter",
          RepositoryId.parse("IDL:hello/RiskyGreeter:1.0"),
          List.of(),
          List.of(),
          List.of(RISKY_GREET));

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
  void callerSuppliedObjectIdsCanBeBoundInvokedUnboundAndReused() {
    LocalOrb orb = LocalOrb.create();
    LocalObjectReference<Greeter> first =
        orb.bindWithObjectId(
            Greeter.class,
            GREETER_DESCRIPTOR,
            "user-id",
            new GreeterDispatcher(new GreeterServant()));

    assertEquals("user-id", first.objectId());
    assertEquals("Hello Ada", orb.invoke(first, GREET, List.of("Ada")));

    orb.unbind("user-id");

    OBJECT_NOT_EXIST staleFailure =
        assertThrows(OBJECT_NOT_EXIST.class, () -> orb.invoke(first, GREET, List.of("Ada")));
    LocalObjectReference<Greeter> second =
        orb.bindWithObjectId(
            Greeter.class,
            GREETER_DESCRIPTOR,
            "user-id",
            request -> "Rebound " + request.arguments().get(0));

    assertEquals(CompletionStatus.COMPLETED_NO, staleFailure.completed);
    assertEquals("Rebound Ada", orb.invoke(second, GREET, List.of("Ada")));
  }

  @Test
  void callerSuppliedObjectIdsRejectInvalidAndDuplicateBindings() {
    LocalOrb orb = LocalOrb.create();
    CountingDispatcher dispatcher = new CountingDispatcher();

    assertThrows(
        BAD_PARAM.class, () -> orb.bindWithObjectId(null, GREETER_DESCRIPTOR, "id", dispatcher));
    assertThrows(
        BAD_PARAM.class, () -> orb.bindWithObjectId(Greeter.class, null, "id", dispatcher));
    assertThrows(
        BAD_PARAM.class,
        () -> orb.bindWithObjectId(Greeter.class, GREETER_DESCRIPTOR, null, dispatcher));
    assertThrows(
        BAD_PARAM.class,
        () -> orb.bindWithObjectId(Greeter.class, GREETER_DESCRIPTOR, " ", dispatcher));
    assertThrows(
        BAD_PARAM.class, () -> orb.bindWithObjectId(Greeter.class, GREETER_DESCRIPTOR, "id", null));

    orb.bindWithObjectId(Greeter.class, GREETER_DESCRIPTOR, "id", dispatcher);

    BAD_PARAM duplicate =
        assertThrows(
            BAD_PARAM.class,
            () -> orb.bindWithObjectId(Greeter.class, GREETER_DESCRIPTOR, "id", dispatcher));

    assertEquals(CompletionStatus.COMPLETED_NO, duplicate.completed);
    assertThrows(BAD_PARAM.class, () -> orb.unbind(null));
    assertThrows(BAD_PARAM.class, () -> orb.unbind(" "));
  }

  @Test
  void staleReferenceFailsDeterministically() {
    LocalObjectReference<Greeter> otherOrbReference =
        LocalOrb.create()
            .bind(Greeter.class, GREETER_DESCRIPTOR, new GreeterDispatcher(new GreeterServant()));
    LocalOrb orb = LocalOrb.create();

    OBJECT_NOT_EXIST exception =
        assertThrows(
            OBJECT_NOT_EXIST.class, () -> orb.invoke(otherOrbReference, GREET, List.of("Ada")));

    assertEquals("Local object reference belongs to a different local ORB", exception.getMessage());
    assertEquals(CompletionStatus.COMPLETED_NO, exception.completed);
  }

  @Test
  void operationAndArgumentValidationHappensBeforeDispatch() {
    LocalOrb orb = LocalOrb.create();
    CountingDispatcher dispatcher = new CountingDispatcher();
    LocalObjectReference<Greeter> reference =
        orb.bind(Greeter.class, GREETER_DESCRIPTOR, dispatcher);

    assertThrows(BAD_OPERATION.class, () -> orb.invoke(reference, FAREWELL, List.of()));
    assertThrows(BAD_PARAM.class, () -> orb.invoke(reference, GREET, List.of()));
    assertEquals(0, dispatcher.count());
  }

  @Test
  void nullInputsMapToBadParamBeforeDispatch() {
    LocalOrb orb = LocalOrb.create();
    CountingDispatcher dispatcher = new CountingDispatcher();
    LocalObjectReference<Greeter> reference =
        orb.bind(Greeter.class, GREETER_DESCRIPTOR, dispatcher);

    BAD_PARAM nullType =
        assertThrows(BAD_PARAM.class, () -> orb.bind(null, GREETER_DESCRIPTOR, dispatcher));
    BAD_PARAM nullReference =
        assertThrows(BAD_PARAM.class, () -> orb.invoke(null, GREET, List.of("Ada")));
    BAD_PARAM nullOperation =
        assertThrows(BAD_PARAM.class, () -> orb.invoke(reference, null, List.of("Ada")));
    BAD_PARAM nullArguments =
        assertThrows(BAD_PARAM.class, () -> orb.invoke(reference, GREET, null));

    assertEquals(CompletionStatus.COMPLETED_NO, nullType.completed);
    assertEquals(CompletionStatus.COMPLETED_NO, nullReference.completed);
    assertEquals(CompletionStatus.COMPLETED_NO, nullOperation.completed);
    assertEquals(CompletionStatus.COMPLETED_NO, nullArguments.completed);
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
    BAD_INV_ORDER bindFailure =
        assertThrows(
            BAD_INV_ORDER.class,
            () ->
                orb.bind(
                    Greeter.class,
                    GREETER_DESCRIPTOR,
                    new GreeterDispatcher(new GreeterServant())));
    BAD_INV_ORDER invokeFailure =
        assertThrows(BAD_INV_ORDER.class, () -> orb.invoke(reference, GREET, List.of("Ada")));

    assertEquals(CompletionStatus.COMPLETED_NO, bindFailure.completed);
    assertEquals(CompletionStatus.COMPLETED_NO, invokeFailure.completed);
  }

  @Test
  void localInitialReferencesCanBeRegisteredResolvedRemovedAndReused() {
    LocalOrb orb = LocalOrb.create();
    GreeterServant first = new GreeterServant();
    GreeterServant second = new GreeterServant();

    orb.registerInitialReference("NameService", GreeterServant.class, first);

    assertSame(first, orb.resolveInitialReference("NameService", GreeterServant.class));
    assertSame(first, orb.resolveInitialReference("NameService", Object.class));

    BAD_PARAM duplicate =
        assertThrows(
            BAD_PARAM.class,
            () -> orb.registerInitialReference("NameService", GreeterServant.class, second));

    assertEquals(CompletionStatus.COMPLETED_NO, duplicate.completed);

    orb.removeInitialReference("NameService");
    orb.registerInitialReference("NameService", GreeterServant.class, second);

    assertSame(second, orb.resolveInitialReference("NameService", GreeterServant.class));
  }

  @Test
  void localInitialReferencesRejectInvalidInputsMissingEntriesWrongTypesAndShutdown() {
    LocalOrb orb = LocalOrb.create();
    GreeterServant servant = new GreeterServant();

    assertThrows(BAD_PARAM.class, () -> orb.registerInitialReference(null, Object.class, servant));
    assertThrows(BAD_PARAM.class, () -> orb.registerInitialReference(" ", Object.class, servant));
    assertThrows(BAD_PARAM.class, () -> orb.registerInitialReference("x", null, servant));
    assertThrows(BAD_PARAM.class, () -> orb.registerInitialReference("x", Object.class, null));
    assertThrows(BAD_PARAM.class, () -> orb.resolveInitialReference("missing", Object.class));
    assertThrows(BAD_PARAM.class, () -> orb.removeInitialReference("missing"));

    orb.registerInitialReference("x", GreeterServant.class, servant);

    assertThrows(BAD_PARAM.class, () -> orb.resolveInitialReference("x", RiskyGreeter.class));

    orb.shutdown();

    assertThrows(BAD_INV_ORDER.class, () -> orb.resolveInitialReference("x", Object.class));
    assertThrows(BAD_INV_ORDER.class, () -> orb.removeInitialReference("x"));
    assertThrows(
        BAD_INV_ORDER.class, () -> orb.registerInitialReference("y", Object.class, servant));
  }

  @Test
  void localInvocationDoesNotRequireNetworkTransportClasses() {
    LocalOrb orb = LocalOrb.create();
    LocalObjectReference<Greeter> reference =
        orb.bind(
            Greeter.class, GREETER_DESCRIPTOR, request -> "local:" + request.arguments().get(0));

    assertEquals("local:Ada", orb.invoke(reference, GREET, List.of("Ada")));
  }

  @Test
  void declaredUserExceptionMappingPreservesOriginalExceptionAndDescriptor() {
    LocalOrb orb = LocalOrb.create();
    BadName badName = new BadName("Ada");
    LocalObjectReference<RiskyGreeter> reference =
        orb.bind(
            RiskyGreeter.class,
            RISKY_GREETER_DESCRIPTOR,
            request -> {
              throw badName;
            });

    LocalInvocationUserException exception =
        assertThrows(
            LocalInvocationUserException.class,
            () -> orb.invoke(reference, RISKY_GREET, List.of("Ada")));

    assertSame(badName, exception.userException());
    assertSame(badName, exception.getCause());
    assertEquals(RISKY_GREET, exception.operation());
    assertEquals(BAD_NAME_TYPE, exception.raisedType());
  }

  @Test
  void generatedStyleClientRethrowsDeclaredUserException() {
    LocalOrb orb = LocalOrb.create();
    BadName badName = new BadName("Ada");
    LocalObjectReference<RiskyGreeter> reference =
        orb.bind(
            RiskyGreeter.class,
            RISKY_GREETER_DESCRIPTOR,
            request -> {
              throw badName;
            });
    RiskyGreeter client = new RiskyGreeterClient(orb, reference);

    assertSame(badName, assertThrows(BadName.class, () -> client.greet("Ada")));
  }

  @Test
  void undeclaredDispatcherExceptionMapsToUnknownWithCause() {
    LocalOrb orb = LocalOrb.create();
    IllegalArgumentException cause = new IllegalArgumentException("boom");
    LocalObjectReference<Greeter> reference =
        orb.bind(
            Greeter.class,
            GREETER_DESCRIPTOR,
            request -> {
              throw cause;
            });

    UNKNOWN exception =
        assertThrows(UNKNOWN.class, () -> orb.invoke(reference, GREET, List.of("Ada")));

    assertSame(cause, exception.getCause());
    assertEquals(CompletionStatus.COMPLETED_MAYBE, exception.completed);
  }

  @Test
  void dispatcherThrownSystemExceptionIsRethrownUnchanged() {
    LocalOrb orb = LocalOrb.create();
    BAD_PARAM badParam = new BAD_PARAM("servant rejected", 5, CompletionStatus.COMPLETED_NO);
    LocalObjectReference<Greeter> reference =
        orb.bind(
            Greeter.class,
            GREETER_DESCRIPTOR,
            request -> {
              throw badParam;
            });

    assertSame(
        badParam,
        assertThrows(BAD_PARAM.class, () -> orb.invoke(reference, GREET, List.of("Ada"))));
  }

  private interface Greeter {

    String greet(String name);
  }

  private interface RiskyGreeter {

    String greet(String name) throws BadName;
  }

  private static final class BadName extends Exception {

    private static final long serialVersionUID = 1L;

    private BadName(String name) {
      super(name);
    }
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

  private static final class RiskyGreeterClient implements RiskyGreeter {

    private final LocalOrb orb;
    private final LocalObjectReference<RiskyGreeter> reference;

    private RiskyGreeterClient(LocalOrb orb, LocalObjectReference<RiskyGreeter> reference) {
      this.orb = orb;
      this.reference = reference;
    }

    @Override
    public String greet(String name) throws BadName {
      try {
        return (String) orb.invoke(reference, RISKY_GREET, List.of(name));
      } catch (LocalInvocationUserException exception) {
        throw assertInstanceOf(BadName.class, exception.userException());
      }
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
