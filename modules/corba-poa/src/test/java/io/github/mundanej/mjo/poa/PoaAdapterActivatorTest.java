package io.github.mundanej.mjo.poa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mundanej.mjo.orb.LocalOrb;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.omg.CORBA.BAD_PARAM;

/** Tests for explicit child POA lookup through an adapter activator. */
@Tag("unit")
final class PoaAdapterActivatorTest {

  @Test
  void findChildCanAskAdapterActivatorToCreateMissingChild() {
    Poa root = Poa.createRoot(LocalOrb.create());
    AtomicInteger activations = new AtomicInteger();
    root.setAdapterActivator(
        (parent, name) -> {
          activations.incrementAndGet();
          return parent.createChild(name, PoaPolicySet.transientRetainedProfile());
        });

    assertNull(root.findChild("generated", false));
    Poa child = root.findChild("generated", true);
    Poa again = root.findChild("generated", true);

    assertEquals("generated", child.name());
    assertEquals("/RootPOA/generated", child.path());
    assertSame(child, again);
    assertEquals(1, activations.get());
  }

  @Test
  void adapterActivatorCannotReturnChildFromAnotherParent() {
    Poa root = Poa.createRoot(LocalOrb.create());
    Poa otherRoot = Poa.createRoot(LocalOrb.create());
    Poa foreignChild = otherRoot.createChild("generated", PoaPolicySet.transientRetainedProfile());
    root.setAdapterActivator((parent, name) -> foreignChild);

    assertThrows(BAD_PARAM.class, () -> root.findChild("generated", true));
    assertNull(root.findChild("generated", false));
  }
}
