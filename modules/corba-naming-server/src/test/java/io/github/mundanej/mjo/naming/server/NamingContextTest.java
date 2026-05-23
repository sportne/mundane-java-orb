package io.github.mundanej.mjo.naming.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjo.naming.NameComponent;
import io.github.mundanej.mjo.naming.NamingBinding;
import io.github.mundanej.mjo.naming.NamingBindingIterator;
import io.github.mundanej.mjo.naming.NamingBindingTarget;
import io.github.mundanej.mjo.naming.NamingContext;
import io.github.mundanej.mjo.naming.NamingDiagnosticCodes;
import io.github.mundanej.mjo.naming.NamingException;
import io.github.mundanej.mjo.naming.NamingListResult;
import io.github.mundanej.mjo.naming.NamingName;
import io.github.mundanej.mjo.orb.LocalObjectReference;
import io.github.mundanej.mjo.orb.LocalOrb;
import io.github.mundanej.mjo.repositoryid.RepositoryId;
import io.github.mundanej.mjo.typecode.IdlGeneratedTypeDescriptor;
import io.github.mundanej.mjo.typecode.IdlTypeKind;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit tests for the local in-memory NamingContext implementation. */
@Tag("unit")
final class NamingContextTest {

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
  void bindsResolvesRebindsAndUnbindsObjectTargets() {
    LocalNamingContext root = LocalNamingContext.createRoot();
    LocalObjectReference<Dummy> first = object("first");
    LocalObjectReference<Dummy> second = object("second");
    NamingName name = NamingName.parse("service");

    root.bind(name, NamingBindingTarget.object(first));
    assertEquals(first, root.resolve(name).objectReference().orElseThrow());

    assertCode(
        NamingDiagnosticCodes.ALREADY_BOUND,
        () -> root.bind(name, NamingBindingTarget.object(second)));

    root.rebind(name, NamingBindingTarget.object(second));
    assertEquals(second, root.resolve(name).objectReference().orElseThrow());

    root.unbind(name);
    assertCode(NamingDiagnosticCodes.NOT_FOUND, () -> root.resolve(name));
  }

  @Test
  void traversesHierarchicalContextsAndRejectsObjectIntermediates() {
    LocalNamingContext root = LocalNamingContext.createRoot();
    NamingContext child = root.bindNewContext(NamingName.parse("apps"));
    LocalObjectReference<Dummy> target = object("target");

    root.bind(NamingName.parse("apps/service"), NamingBindingTarget.object(target));

    assertSame(child, root.resolve(NamingName.parse("apps")).context().orElseThrow());
    assertEquals(
        target, root.resolve(NamingName.parse("apps/service")).objectReference().orElseThrow());

    root.bind(NamingName.parse("object"), NamingBindingTarget.object(object("object")));

    assertCode(
        NamingDiagnosticCodes.NOT_CONTEXT, () -> root.resolve(NamingName.parse("object/leaf")));
    assertCode(
        NamingDiagnosticCodes.NOT_FOUND,
        () -> root.bind(NamingName.parse("missing/leaf"), NamingBindingTarget.object(target)));
  }

  @Test
  void rebindCreatesMissingLeafAndReplacesExistingContextLeaf() {
    LocalNamingContext root = LocalNamingContext.createRoot();
    LocalObjectReference<Dummy> object = object("object");

    root.bindNewContext(NamingName.parse("apps"));
    root.rebind(NamingName.parse("apps/service"), NamingBindingTarget.object(object));
    assertEquals(
        object, root.resolve(NamingName.parse("apps/service")).objectReference().orElseThrow());

    root.rebind(NamingName.parse("apps"), NamingBindingTarget.object(object("replacement")));

    assertEquals(NamingBindingTarget.Kind.OBJECT, root.resolve(NamingName.parse("apps")).kind());
    assertCode(
        NamingDiagnosticCodes.NOT_CONTEXT, () -> root.resolve(NamingName.parse("apps/service")));
  }

  @Test
  void rejectsDestroyedAndNonLocalContextIntermediates() {
    LocalNamingContext root = LocalNamingContext.createRoot();
    NamingContext destroyedChild = root.bindNewContext(NamingName.parse("destroyed"));

    destroyedChild.destroy();
    root.bind(NamingName.parse("remote"), NamingBindingTarget.context(new NonLocalContext()));

    assertCode(
        NamingDiagnosticCodes.DESTROYED, () -> root.resolve(NamingName.parse("destroyed/x")));
    assertCode(NamingDiagnosticCodes.NOT_CONTEXT, () -> root.resolve(NamingName.parse("remote/x")));
  }

  @Test
  void listsInlineBindingsAndIteratorRemainderFromSnapshot() {
    LocalNamingContext root = LocalNamingContext.createRoot();
    root.bind(NamingName.parse("a"), NamingBindingTarget.object(object("a")));
    root.bind(NamingName.parse("b"), NamingBindingTarget.object(object("b")));
    root.bind(NamingName.parse("c"), NamingBindingTarget.object(object("c")));

    NamingListResult listed = root.list(1);

    assertEquals(List.of(new NameComponent("a", "")), names(listed.bindings()));
    assertTrue(listed.iterator().isPresent());
    NamingBindingIterator iterator = listed.iterator().orElseThrow();

    root.unbind(NamingName.parse("b"));

    assertEquals(List.of(new NameComponent("b", "")), names(iterator.next(1)));
    assertEquals(new NameComponent("c", ""), iterator.nextOne().orElseThrow().name());
    assertFalse(iterator.nextOne().isPresent());
    iterator.destroy();
    assertCode(NamingDiagnosticCodes.ITERATOR_CLOSED, iterator::nextOne);
    assertThrows(
        UnsupportedOperationException.class, () -> listed.bindings().add(listed.bindings().get(0)));
  }

  @Test
  void listZeroReturnsIteratorOverSnapshotAndIteratorBatchesAreImmutable() {
    LocalNamingContext root = LocalNamingContext.createRoot();
    root.bind(NamingName.parse("a"), NamingBindingTarget.object(object("a")));
    root.bind(NamingName.parse("b"), NamingBindingTarget.object(object("b")));

    NamingBindingIterator iterator = root.list(0).iterator().orElseThrow();

    assertEquals(List.of(), iterator.next(0));
    List<NamingBinding> batch = iterator.next(2);
    assertEquals(List.of(new NameComponent("a", ""), new NameComponent("b", "")), names(batch));
    assertThrows(UnsupportedOperationException.class, () -> batch.add(batch.get(0)));
    assertEquals(List.of(), iterator.next(1));
    assertCode(NamingDiagnosticCodes.INVALID_NAME, () -> iterator.next(-1));
    iterator.destroy();
    iterator.destroy();
    assertCode(NamingDiagnosticCodes.ITERATOR_CLOSED, () -> iterator.next(0));
  }

  @Test
  void destroysOnlyEmptyContextsAndRejectsDestroyedUse() {
    LocalNamingContext root = LocalNamingContext.createRoot();
    NamingContext child = root.bindNewContext(NamingName.parse("child"));

    assertCode(NamingDiagnosticCodes.NOT_EMPTY, root::destroy);

    root.unbind(NamingName.parse("child"));
    child.destroy();

    assertTrue(child.isDestroyed());
    assertCode(NamingDiagnosticCodes.DESTROYED, () -> child.list(0));
  }

  @Test
  void rejectsInvalidListCounts() {
    LocalNamingContext root = LocalNamingContext.createRoot();
    NamingListResult empty = root.list(10);

    assertEquals(List.of(), empty.bindings());
    assertTrue(empty.iterator().isEmpty());
    assertCode(NamingDiagnosticCodes.INVALID_NAME, () -> root.list(-1));
  }

  private static List<NameComponent> names(List<NamingBinding> bindings) {
    return bindings.stream().map(NamingBinding::name).toList();
  }

  private static LocalObjectReference<Dummy> object(String id) {
    return LocalOrb.create().bindWithObjectId(Dummy.class, DUMMY_DESCRIPTOR, id, request -> null);
  }

  private static void assertCode(Object expectedCode, ThrowingRunnable runnable) {
    NamingException exception = assertThrows(NamingException.class, runnable::run);
    assertEquals(expectedCode, exception.code());
  }

  private interface Dummy {}

  private static final class NonLocalContext implements NamingContext {

    @Override
    public void bind(NamingName name, NamingBindingTarget target) {}

    @Override
    public void rebind(NamingName name, NamingBindingTarget target) {}

    @Override
    public NamingBindingTarget resolve(NamingName name) {
      throw new AssertionError("non-local context should not be traversed");
    }

    @Override
    public void unbind(NamingName name) {}

    @Override
    public NamingContext bindNewContext(NamingName name) {
      return this;
    }

    @Override
    public NamingListResult list(int howMany) {
      return new NamingListResult(List.of(), java.util.Optional.empty());
    }

    @Override
    public void destroy() {}

    @Override
    public boolean isDestroyed() {
      return false;
    }
  }

  @FunctionalInterface
  private interface ThrowingRunnable {

    void run();
  }
}
