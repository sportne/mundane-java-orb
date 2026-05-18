package io.github.mundanej.mjo.naming.server;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjo.naming.NamingContext;
import io.github.mundanej.mjo.orb.LocalOrb;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.omg.CORBA.BAD_INV_ORDER;
import org.omg.CORBA.BAD_PARAM;

/** Integration tests for local Naming Service installation. */
@Tag("unit")
final class LocalNamingServiceTest {

  @Test
  void installsNameServiceInitialReference() {
    LocalOrb orb = LocalOrb.create();

    NamingContext root = LocalNamingService.install(orb);

    assertSame(
        root, orb.resolveInitialReference(LocalNamingService.NAME_SERVICE, NamingContext.class));
    assertThrows(BAD_PARAM.class, () -> LocalNamingService.install(orb));
  }

  @Test
  void localOrbCanRemoveAndReinstallNameService() {
    LocalOrb orb = LocalOrb.create();
    NamingContext first = LocalNamingService.install(orb);

    orb.removeInitialReference(LocalNamingService.NAME_SERVICE);
    NamingContext second = LocalNamingService.install(orb);

    assertTrue(first != second);
    assertSame(
        second, orb.resolveInitialReference(LocalNamingService.NAME_SERVICE, NamingContext.class));
  }

  @Test
  void shutdownClearsNameServiceInitialReferenceAndRejectsNewInstallation() {
    LocalOrb orb = LocalOrb.create();
    LocalNamingService.install(orb);

    orb.shutdown();

    assertThrows(
        BAD_INV_ORDER.class,
        () -> orb.resolveInitialReference(LocalNamingService.NAME_SERVICE, NamingContext.class));
    assertThrows(BAD_INV_ORDER.class, () -> LocalNamingService.install(orb));
  }
}
