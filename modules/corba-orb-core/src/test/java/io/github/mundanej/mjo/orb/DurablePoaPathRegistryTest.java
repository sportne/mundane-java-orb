package io.github.mundanej.mjo.orb;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.omg.CORBA.BAD_INV_ORDER;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.OBJECT_NOT_EXIST;

/** Tests for the durable POA path registry owned by LocalOrb. */
@Tag("unit")
final class DurablePoaPathRegistryTest {

  @Test
  void durableOrbRegistersAndRequiresApprovedPoaPaths() {
    LocalOrb orb = LocalOrb.create(OrbIdentity.durable("registry-orb"));
    DurablePoaPathRegistry registry = orb.durablePoaPaths();
    DurableObjectKey key =
        DurableObjectKey.fromPoaPath("registry-orb", "/RootPOA/apps", ascii("alpha"), 0);

    assertFalse(registry.contains(List.of("RootPOA", "apps")));

    registry.register(List.of("RootPOA", "apps"));

    assertTrue(registry.contains(List.of("RootPOA", "apps")));
    registry.requireRegistered(key);
  }

  @Test
  void durableRegistryRejectsDuplicateWrongOrbAndUnregisteredPaths() {
    LocalOrb orb = LocalOrb.create(OrbIdentity.durable("registry-orb"));
    DurablePoaPathRegistry registry = orb.durablePoaPaths();
    DurableObjectKey wrongOrb =
        DurableObjectKey.fromPoaPath("other-orb", "/RootPOA/apps", ascii("alpha"), 0);
    DurableObjectKey unregistered =
        DurableObjectKey.fromPoaPath("registry-orb", "/RootPOA/missing", ascii("alpha"), 0);

    registry.register(List.of("RootPOA", "apps"));

    assertThrows(BAD_PARAM.class, () -> registry.register(List.of("RootPOA", "apps")));
    assertThrows(OBJECT_NOT_EXIST.class, () -> registry.requireRegistered(wrongOrb));
    assertThrows(OBJECT_NOT_EXIST.class, () -> registry.requireRegistered(unregistered));
    assertThrows(OBJECT_NOT_EXIST.class, () -> registry.unregister(List.of("RootPOA", "missing")));
  }

  @Test
  void registryRejectsTransientOrbAndHostilePaths() {
    DurablePoaPathRegistry transientRegistry = LocalOrb.create().durablePoaPaths();
    DurablePoaPathRegistry registry =
        LocalOrb.create(OrbIdentity.durable("registry-orb")).durablePoaPaths();

    assertThrows(BAD_PARAM.class, () -> transientRegistry.register(List.of("RootPOA")));
    assertThrows(BAD_PARAM.class, () -> registry.register(List.of()));
    assertThrows(BAD_PARAM.class, () -> registry.register(null));
    assertThrows(BAD_PARAM.class, () -> registry.register(Collections.singletonList(null)));
    assertThrows(BAD_PARAM.class, () -> registry.register(List.of("RootPOA", "..")));
    assertThrows(BAD_PARAM.class, () -> registry.register(List.of("RootPOA", "a/b")));
    assertThrows(BAD_PARAM.class, () -> registry.register(List.of("RootPOA", "x".repeat(129))));
  }

  @Test
  void registryClearsAndRejectsUseAfterOrbShutdown() {
    LocalOrb orb = LocalOrb.create(OrbIdentity.durable("registry-orb"));
    DurablePoaPathRegistry registry = orb.durablePoaPaths();

    registry.register(List.of("RootPOA"));
    orb.shutdown();

    assertThrows(BAD_INV_ORDER.class, () -> registry.contains(List.of("RootPOA")));
    assertThrows(BAD_INV_ORDER.class, () -> registry.register(List.of("RootPOA", "apps")));
    assertThrows(
        BAD_INV_ORDER.class,
        () ->
            registry.requireRegistered(
                DurableObjectKey.fromPoaPath("registry-orb", "/RootPOA", ascii("alpha"), 0)));
  }

  private static byte[] ascii(String value) {
    return value.getBytes(StandardCharsets.US_ASCII);
  }
}
