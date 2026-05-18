package io.github.mundanej.mjo.naming.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mundanej.mjo.ior.CorbanameUrl;
import io.github.mundanej.mjo.naming.NamingBindingTarget;
import io.github.mundanej.mjo.naming.NamingContext;
import io.github.mundanej.mjo.naming.NamingDiagnosticCodes;
import io.github.mundanej.mjo.naming.NamingException;
import io.github.mundanej.mjo.naming.NamingName;
import io.github.mundanej.mjo.orb.LocalObjectReference;
import io.github.mundanej.mjo.orb.LocalOrb;
import io.github.mundanej.mjo.repositoryid.RepositoryId;
import io.github.mundanej.mjo.typecode.IdlGeneratedTypeDescriptor;
import io.github.mundanej.mjo.typecode.IdlTypeKind;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.omg.CORBA.BAD_PARAM;

/** Tests for local corbaname:rir: resolution through NameService. */
@Tag("unit")
final class CorbanameResolverTest {

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
  void resolvesEmptyRirCorbanameToRootNamingContext() {
    LocalOrb orb = LocalOrb.create();
    NamingContext root = LocalNamingService.install(orb);

    NamingBindingTarget target =
        new CorbanameResolver(orb).resolve(CorbanameUrl.parse("corbaname:rir:"));

    assertEquals(NamingBindingTarget.Kind.CONTEXT, target.kind());
    assertSame(root, target.context().orElseThrow());
  }

  @Test
  void resolvesPercentDecodedStringifiedNameThroughRootContext() {
    LocalOrb orb = LocalOrb.create();
    NamingContext root = LocalNamingService.install(orb);
    LocalObjectReference<Dummy> object =
        orb.bindWithObjectId(Dummy.class, DUMMY_DESCRIPTOR, "dummy", request -> null);
    root.bindNewContext(NamingName.parse("a"));
    root.bindNewContext(NamingName.parse("a/path to"));
    root.bind(NamingName.parse("a/path to/obj"), NamingBindingTarget.object(object));

    NamingBindingTarget target =
        new CorbanameResolver(orb).resolve(CorbanameUrl.parse("corbaname:rir:#a/path%20to/obj"));

    assertEquals(object, target.objectReference().orElseThrow());
  }

  @Test
  void rejectsUnsupportedLocationsMissingNameServiceAndMissingObjects() {
    LocalOrb orb = LocalOrb.create();
    CorbanameResolver resolver = new CorbanameResolver(orb);

    assertThrows(BAD_PARAM.class, () -> resolver.resolve(CorbanameUrl.parse("corbaname:rir:#x")));

    LocalNamingService.install(orb);

    assertCode(
        NamingDiagnosticCodes.UNSUPPORTED_LOCATION,
        () -> resolver.resolve(CorbanameUrl.parse("corbaname::localhost#x")));
    assertCode(
        NamingDiagnosticCodes.NOT_FOUND,
        () -> resolver.resolve(CorbanameUrl.parse("corbaname:rir:#missing")));
  }

  private static void assertCode(Object expectedCode, ThrowingRunnable runnable) {
    NamingException exception = assertThrows(NamingException.class, runnable::run);
    assertEquals(expectedCode, exception.code());
  }

  private interface Dummy {}

  @FunctionalInterface
  private interface ThrowingRunnable {

    void run();
  }
}
