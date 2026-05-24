package org.omg.PortableServer;

import org.omg.PortableServer.POAManagerPackage.AdapterInactive;

/** POA manager compatibility surface. */
public interface POAManager {

  /** Activates managed POAs. */
  void activate() throws AdapterInactive;

  /** Holds requests for managed POAs. */
  void hold_requests(boolean waitForCompletion) throws AdapterInactive;

  /** Discards requests for managed POAs. */
  void discard_requests(boolean waitForCompletion) throws AdapterInactive;

  /** Deactivates managed POAs. */
  void deactivate(boolean etherealizeObjects, boolean waitForCompletion) throws AdapterInactive;

  /** Returns the manager state. */
  State get_state();
}
