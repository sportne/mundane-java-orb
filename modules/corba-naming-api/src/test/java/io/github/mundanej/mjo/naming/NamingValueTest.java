package io.github.mundanej.mjo.naming;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mundanej.mjo.common.DiagnosticCode;
import io.github.mundanej.mjo.orb.LocalObjectReference;
import io.github.mundanej.mjo.orb.LocalOrb;
import io.github.mundanej.mjo.repositoryid.RepositoryId;
import io.github.mundanej.mjo.typecode.IdlGeneratedTypeDescriptor;
import io.github.mundanej.mjo.typecode.IdlTypeKind;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit tests for local naming value records. */
@Tag("unit")
final class NamingValueTest {

  private static final IdlGeneratedTypeDescriptor DUMMY_DESCRIPTOR =
      new IdlGeneratedTypeDescriptor(
          IdlTypeKind.INTERFACE,
          "::demo::Dummy",
          Dummy.class.getName(),
          RepositoryId.parse("IDL:demo/Dummy:1.0"),
          List.of(),
          List.of(),
          List.of());

  @Test
  void createsObjectAndContextTargets() {
    LocalObjectReference<Dummy> object =
        LocalOrb.create().bind(Dummy.class, DUMMY_DESCRIPTOR, request -> null);
    NamingContext context = new RecordingContext();

    NamingBindingTarget objectTarget = NamingBindingTarget.object(object);
    NamingBindingTarget contextTarget = NamingBindingTarget.context(context);

    assertEquals(NamingBindingTarget.Kind.OBJECT, objectTarget.kind());
    assertEquals(Optional.of(object), objectTarget.objectReference());
    assertEquals(Optional.empty(), objectTarget.context());
    assertEquals(NamingBindingTarget.Kind.CONTEXT, contextTarget.kind());
    assertEquals(Optional.empty(), contextTarget.objectReference());
    assertEquals(Optional.of(context), contextTarget.context());
  }

  @Test
  void rejectsInvalidTargetShapesAndNulls() {
    LocalObjectReference<Dummy> object =
        LocalOrb.create().bind(Dummy.class, DUMMY_DESCRIPTOR, request -> null);
    NamingContext context = new RecordingContext();

    assertThrows(NullPointerException.class, () -> NamingBindingTarget.object(null));
    assertThrows(NullPointerException.class, () -> NamingBindingTarget.context(null));
    assertThrows(
        NullPointerException.class,
        () -> new NamingBindingTarget(null, Optional.of(object), Optional.empty()));
    assertThrows(
        NamingException.class,
        () ->
            new NamingBindingTarget(
                NamingBindingTarget.Kind.OBJECT, Optional.empty(), Optional.empty()));
    assertThrows(
        NamingException.class,
        () ->
            new NamingBindingTarget(
                NamingBindingTarget.Kind.OBJECT, Optional.of(object), Optional.of(context)));
    assertThrows(
        NamingException.class,
        () ->
            new NamingBindingTarget(
                NamingBindingTarget.Kind.CONTEXT, Optional.of(object), Optional.of(context)));
    assertThrows(
        NamingException.class,
        () ->
            new NamingBindingTarget(
                NamingBindingTarget.Kind.CONTEXT, Optional.empty(), Optional.empty()));

    NamingException invalidObject =
        assertThrows(
            NamingException.class,
            () ->
                new NamingBindingTarget(
                    NamingBindingTarget.Kind.OBJECT, Optional.empty(), Optional.empty()));

    assertEquals(NamingDiagnosticCodes.INVALID_NAME, invalidObject.code());
  }

  @Test
  void createsBindingAndImmutableListResult() {
    NamingBinding binding =
        new NamingBinding(
            NameComponent.id("service"), NamingBindingTarget.context(new RecordingContext()));
    NamingListResult result = new NamingListResult(List.of(binding), Optional.empty());

    assertEquals(NameComponent.id("service"), binding.name());
    assertSame(binding, binding);
    assertEquals(List.of(binding), result.bindings());
    assertThrows(UnsupportedOperationException.class, () -> result.bindings().add(binding));
    assertThrows(NullPointerException.class, () -> new NamingBinding(null, binding.target()));
    assertThrows(NullPointerException.class, () -> new NamingBinding(binding.name(), null));
    assertThrows(NullPointerException.class, () -> new NamingListResult(null, Optional.empty()));
    assertThrows(NullPointerException.class, () -> new NamingListResult(List.of(), null));
  }

  @Test
  void coversNamingNameFactoryAndExceptionValidation() {
    NamingName name = NamingName.of(NameComponent.id("a"), new NameComponent("b", "kind"));
    NamingException exception =
        new NamingException(NamingDiagnosticCodes.NOT_FOUND, "missing name");

    assertEquals("a/b.kind", name.stringified());
    assertEquals(List.of(), NamingName.of(NameComponent.id("a")).parentComponents());
    assertEquals(NamingDiagnosticCodes.NOT_FOUND, exception.code());
    assertThrows(NullPointerException.class, () -> namingException(null, "message"));
    assertThrows(
        IllegalArgumentException.class,
        () -> namingException(NamingDiagnosticCodes.INVALID_NAME, " "));
  }

  private static NamingException namingException(DiagnosticCode code, String message) {
    return new NamingException(code, message);
  }

  private interface Dummy {}

  private static final class RecordingContext implements NamingContext {

    @Override
    public void bind(NamingName name, NamingBindingTarget target) {}

    @Override
    public void rebind(NamingName name, NamingBindingTarget target) {}

    @Override
    public NamingBindingTarget resolve(NamingName name) {
      return NamingBindingTarget.context(this);
    }

    @Override
    public void unbind(NamingName name) {}

    @Override
    public NamingContext bindNewContext(NamingName name) {
      return this;
    }

    @Override
    public NamingListResult list(int howMany) {
      return new NamingListResult(List.of(), Optional.empty());
    }

    @Override
    public void destroy() {}

    @Override
    public boolean isDestroyed() {
      return false;
    }
  }
}
