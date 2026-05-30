package io.github.mundanej.mjo.naming.server;

import io.github.mundanej.mjo.common.BoundedLimit;
import io.github.mundanej.mjo.naming.NamingDiagnosticCodes;
import io.github.mundanej.mjo.naming.NamingException;
import io.github.mundanej.mjo.orb.OrbIdentity;
import java.nio.file.Path;
import java.util.Objects;

/** Caller-supplied persistence configuration for the network Naming Service. */
public record NamingPersistenceOptions(
    OrbIdentity orbIdentity,
    Path storePath,
    BoundedLimit storeOctets,
    BoundedLimit stringOctets,
    BoundedLimit contextCount,
    BoundedLimit bindingCount) {

  private static final long DEFAULT_STORE_OCTETS = 1_048_576L;
  private static final long DEFAULT_STRING_OCTETS = 65_536L;
  private static final long DEFAULT_CONTEXT_COUNT = 4_096L;
  private static final long DEFAULT_BINDING_COUNT = 65_536L;

  /** Creates validated persistence options. */
  public NamingPersistenceOptions {
    Objects.requireNonNull(orbIdentity, "orbIdentity");
    if (!orbIdentity.durable()) {
      throw new NamingException(
          NamingDiagnosticCodes.UNSUPPORTED_LOCATION,
          "Naming persistence requires a durable ORB identity");
    }
    storePath = validateStorePath(storePath);
    Objects.requireNonNull(storeOctets, "storeOctets");
    Objects.requireNonNull(stringOctets, "stringOctets");
    Objects.requireNonNull(contextCount, "contextCount");
    Objects.requireNonNull(bindingCount, "bindingCount");
  }

  /** Creates default bounded persistence options for a durable ORB identity and store file. */
  public static NamingPersistenceOptions of(OrbIdentity orbIdentity, Path storePath) {
    return new NamingPersistenceOptions(
        orbIdentity,
        storePath,
        new BoundedLimit("naming-store-octets", DEFAULT_STORE_OCTETS),
        new BoundedLimit("naming-store-string-octets", DEFAULT_STRING_OCTETS),
        new BoundedLimit("naming-store-contexts", DEFAULT_CONTEXT_COUNT),
        new BoundedLimit("naming-store-bindings", DEFAULT_BINDING_COUNT));
  }

  private static Path validateStorePath(Path path) {
    Objects.requireNonNull(path, "storePath");
    if (path.getFileName() == null) {
      throw new NamingException(NamingDiagnosticCodes.INVALID_NAME, "store path has no file name");
    }
    for (Path component : path) {
      String value = component.toString();
      if (".".equals(value) || "..".equals(value)) {
        throw new NamingException(
            NamingDiagnosticCodes.INVALID_NAME, "store path must not contain traversal components");
      }
    }
    return path;
  }
}
