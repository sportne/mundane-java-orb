package io.github.mundanej.mjo.naming.server;

import io.github.mundanej.mjo.common.BoundedLimit;
import io.github.mundanej.mjo.common.LimitViolation;
import io.github.mundanej.mjo.ior.IiopProfile;
import io.github.mundanej.mjo.ior.Ior;
import io.github.mundanej.mjo.ior.StringifiedIor;
import io.github.mundanej.mjo.naming.NameComponent;
import io.github.mundanej.mjo.naming.NamingDiagnosticCodes;
import io.github.mundanej.mjo.naming.NamingException;
import io.github.mundanej.mjo.orb.DurableObjectKey;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Bounded binary store for durable network Naming Service state. */
final class NamingPersistenceStore {

  private static final byte[] MAGIC = {'M', 'J', 'N', 'S'};
  private static final int VERSION = 1;
  private static final int KIND_OBJECT = 0;
  private static final int KIND_CONTEXT = 1;
  private static final String NAMING_CONTEXT_REPOSITORY_ID =
      "IDL:omg.org/CosNaming/NamingContextExt:1.0";
  private static final List<String> NAMING_CONTEXT_POA_PATH = List.of("RootPOA", "NameService");

  private final NamingPersistenceOptions options;

  NamingPersistenceStore(NamingPersistenceOptions options) {
    this.options = Objects.requireNonNull(options, "options");
  }

  boolean exists() {
    return Files.exists(options.storePath());
  }

  StoredState load() {
    Path path = options.storePath();
    if (!Files.exists(path)) {
      return StoredState.empty();
    }
    rejectDirectory(path);
    try {
      long size = Files.size(path);
      requireWithin(options.storeOctets(), size);
      byte[] bytes = Files.readAllBytes(path);
      Reader reader = new Reader(bytes);
      reader.requireMagic();
      int version = reader.readUnsignedByte("version");
      if (version != VERSION) {
        throw corrupted("unsupported Naming store version: " + version);
      }
      String orbId = reader.readString("orbId");
      if (!options.orbIdentity().requireDurableOrbId().equals(orbId)) {
        throw corrupted("Naming store ORB id does not match configured identity");
      }
      int nextContextId = reader.readInt("next context id");
      if (nextContextId < 1) {
        throw corrupted("Naming store next context id is invalid");
      }
      int contextCount = reader.readUnsignedShort("context count");
      requireWithin(options.contextCount(), contextCount);
      List<StoredContext> contexts = new ArrayList<>(contextCount);
      for (int index = 0; index < contextCount; index++) {
        Ior contextIor =
            readDurableNamingContextIor(reader.readString("context IOR"), "context IOR");
        boolean destroyed = reader.readBoolean("context destroyed");
        int bindingCount = reader.readUnsignedShort("binding count");
        requireWithin(options.bindingCount(), bindingCount);
        List<StoredBinding> bindings = new ArrayList<>(bindingCount);
        for (int bindingIndex = 0; bindingIndex < bindingCount; bindingIndex++) {
          NameComponent name =
              new NameComponent(reader.readString("name id"), reader.readString("name kind"));
          int kind = reader.readUnsignedByte("target kind");
          RemoteNamingBindingTarget.Kind targetKind =
              switch (kind) {
                case KIND_OBJECT -> RemoteNamingBindingTarget.Kind.OBJECT;
                case KIND_CONTEXT -> RemoteNamingBindingTarget.Kind.CONTEXT;
                default -> throw corrupted("Naming store target kind is invalid: " + kind);
              };
          Ior targetIor = readDurableIor(reader.readString("target IOR"), "target IOR");
          bindings.add(
              new StoredBinding(name, new RemoteNamingBindingTarget(targetKind, targetIor)));
        }
        contexts.add(new StoredContext(contextIor, destroyed, List.copyOf(bindings)));
      }
      reader.requireFullyRead();
      return new StoredState(nextContextId, List.copyOf(contexts));
    } catch (IOException exception) {
      throw new NamingException(
          NamingDiagnosticCodes.INVALID_NAME,
          "failed to read Naming persistence store: " + exception.getMessage());
    }
  }

  void save(StoredState state) {
    Objects.requireNonNull(state, "state");
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    output.writeBytes(MAGIC);
    output.write(VERSION);
    writeString(output, options.orbIdentity().requireDurableOrbId());
    writeInt(output, state.nextContextId());
    writeUnsignedShort(output, state.contexts().size());
    for (StoredContext context : state.contexts()) {
      writeString(output, durableStringifiedNamingContextIor(context.ior(), "context IOR"));
      output.write(context.destroyed() ? 1 : 0);
      writeUnsignedShort(output, context.bindings().size());
      for (StoredBinding binding : context.bindings()) {
        writeString(output, binding.name().id());
        writeString(output, binding.name().kind());
        output.write(
            binding.target().kind() == RemoteNamingBindingTarget.Kind.OBJECT
                ? KIND_OBJECT
                : KIND_CONTEXT);
        writeString(output, durableStringifiedIor(binding.target().ior(), "target IOR"));
      }
    }
    byte[] bytes = output.toByteArray();
    requireWithin(options.storeOctets(), bytes.length);
    Path storePath = options.storePath();
    rejectDirectory(storePath);
    Path parent = storePath.toAbsolutePath().getParent();
    Path temp = storePath.resolveSibling(storePath.getFileName() + ".tmp");
    try {
      if (parent != null) {
        Files.createDirectories(parent);
      }
      writeDurableTempFile(temp, bytes);
      replaceStore(temp, storePath, true);
    } catch (IOException exception) {
      cleanupTempFile(temp);
      throw new NamingException(
          NamingDiagnosticCodes.INVALID_NAME,
          "failed to write Naming persistence store: " + exception.getMessage());
    }
  }

  private static void rejectDirectory(Path path) {
    if (Files.isDirectory(path)) {
      throw new NamingException(
          NamingDiagnosticCodes.INVALID_NAME, "Naming persistence store path is a directory");
    }
  }

  private static void writeDurableTempFile(Path temp, byte[] bytes) throws IOException {
    try (FileChannel channel =
        FileChannel.open(
            temp,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE)) {
      ByteBuffer buffer = ByteBuffer.wrap(bytes);
      while (buffer.hasRemaining()) {
        channel.write(buffer);
      }
      channel.force(true);
    }
  }

  static void replaceStore(Path temp, Path storePath, boolean attemptAtomicMove)
      throws IOException {
    if (attemptAtomicMove) {
      try {
        Files.move(
            temp, storePath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        return;
      } catch (IOException atomicFailure) {
        // Fall through to a bounded non-atomic replacement on filesystems without atomic moves.
      }
    }
    Files.move(temp, storePath, StandardCopyOption.REPLACE_EXISTING);
  }

  private static void cleanupTempFile(Path temp) {
    try {
      Files.deleteIfExists(temp);
    } catch (IOException ignored) {
      // Cleanup is best-effort; the original write failure remains the surfaced diagnostic.
    }
  }

  private Ior readDurableIor(String stringifiedIor, String label) {
    try {
      return requireDurableIor(StringifiedIor.parse(stringifiedIor), label);
    } catch (IllegalArgumentException exception) {
      throw corrupted("invalid " + label + ": " + exception.getMessage());
    }
  }

  private Ior readDurableNamingContextIor(String stringifiedIor, String label) {
    try {
      return requireDurableNamingContextIor(StringifiedIor.parse(stringifiedIor), label);
    } catch (IllegalArgumentException exception) {
      throw corrupted("invalid " + label + ": " + exception.getMessage());
    }
  }

  private String durableStringifiedIor(Ior ior, String label) {
    return StringifiedIor.format(requireDurableIor(ior, label));
  }

  private String durableStringifiedNamingContextIor(Ior ior, String label) {
    return StringifiedIor.format(requireDurableNamingContextIor(ior, label));
  }

  Ior requireDurableIor(Ior ior, String label) {
    durableObjectKey(ior, label);
    return ior;
  }

  private Ior requireDurableNamingContextIor(Ior ior, String label) {
    DurableObjectKey key = durableObjectKey(ior, label);
    if (!NAMING_CONTEXT_REPOSITORY_ID.equals(ior.typeId())) {
      throw corrupted(label + " is not a NamingContext IOR");
    }
    if (!NAMING_CONTEXT_POA_PATH.equals(key.poaPath())) {
      throw corrupted(label + " is outside the NamingContext durable key namespace");
    }
    return ior;
  }

  private DurableObjectKey durableObjectKey(Ior ior, String label) {
    Objects.requireNonNull(ior, label);
    IiopProfile profile =
        ior.profiles().stream()
            .map(tagged -> tagged.internetIopProfile().orElse(null))
            .filter(Objects::nonNull)
            .findFirst()
            .orElseThrow(() -> corrupted(label + " does not contain an IIOP profile"));
    byte[] key = profile.objectKey().octets();
    if (!DurableObjectKey.hasDurablePrefix(key)) {
      throw corrupted(label + " does not contain a durable object key");
    }
    DurableObjectKey decoded;
    try {
      decoded = DurableObjectKey.decode(key);
    } catch (IllegalArgumentException exception) {
      throw corrupted(label + " contains a malformed durable object key");
    }
    if (!options.orbIdentity().requireDurableOrbId().equals(decoded.orbId())) {
      throw corrupted(label + " durable object key belongs to a different ORB");
    }
    return decoded;
  }

  private void writeString(ByteArrayOutputStream output, String value) {
    byte[] bytes = Objects.requireNonNull(value, "value").getBytes(StandardCharsets.UTF_8);
    requireWithin(options.stringOctets(), bytes.length);
    writeInt(output, bytes.length);
    output.writeBytes(bytes);
  }

  private static void writeUnsignedShort(ByteArrayOutputStream output, int value) {
    if (value < 0 || value > 0xFFFF) {
      throw new NamingException(NamingDiagnosticCodes.INVALID_NAME, "value exceeds unsigned short");
    }
    output.write((value >>> 8) & 0xFF);
    output.write(value & 0xFF);
  }

  private static void writeInt(ByteArrayOutputStream output, int value) {
    output.write((value >>> 24) & 0xFF);
    output.write((value >>> 16) & 0xFF);
    output.write((value >>> 8) & 0xFF);
    output.write(value & 0xFF);
  }

  private void requireWithin(BoundedLimit limit, long value) {
    limit.check(value).ifPresent(NamingPersistenceStore::throwLimitExceeded);
  }

  private static void throwLimitExceeded(LimitViolation violation) {
    throw new NamingException(NamingDiagnosticCodes.INVALID_NAME, violation.message());
  }

  private static NamingException corrupted(String message) {
    return new NamingException(NamingDiagnosticCodes.INVALID_NAME, message);
  }

  record StoredState(int nextContextId, List<StoredContext> contexts) {

    static StoredState empty() {
      return new StoredState(1, List.of());
    }

    StoredState {
      contexts = List.copyOf(Objects.requireNonNull(contexts, "contexts"));
    }
  }

  record StoredContext(Ior ior, boolean destroyed, List<StoredBinding> bindings) {

    StoredContext {
      Objects.requireNonNull(ior, "ior");
      bindings = List.copyOf(Objects.requireNonNull(bindings, "bindings"));
    }
  }

  record StoredBinding(NameComponent name, RemoteNamingBindingTarget target) {

    StoredBinding {
      Objects.requireNonNull(name, "name");
      Objects.requireNonNull(target, "target");
    }
  }

  private final class Reader {

    private final byte[] bytes;
    private int offset;

    private Reader(byte[] bytes) {
      this.bytes = Objects.requireNonNull(bytes, "bytes");
    }

    private void requireMagic() {
      byte[] actual = readOctets(MAGIC.length, "magic");
      for (int index = 0; index < MAGIC.length; index++) {
        if (actual[index] != MAGIC[index]) {
          throw corrupted("Naming store magic is invalid");
        }
      }
    }

    private int readUnsignedByte(String label) {
      requireRemaining(1, label);
      return bytes[offset++] & 0xFF;
    }

    private boolean readBoolean(String label) {
      int value = readUnsignedByte(label);
      if (value == 0) {
        return false;
      }
      if (value == 1) {
        return true;
      }
      throw corrupted(label + " boolean value is invalid");
    }

    private int readUnsignedShort(String label) {
      requireRemaining(2, label);
      int value = ((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF);
      offset += 2;
      return value;
    }

    private int readInt(String label) {
      requireRemaining(4, label);
      int value =
          ((bytes[offset] & 0xFF) << 24)
              | ((bytes[offset + 1] & 0xFF) << 16)
              | ((bytes[offset + 2] & 0xFF) << 8)
              | (bytes[offset + 3] & 0xFF);
      offset += 4;
      return value;
    }

    private String readString(String label) {
      int length = readInt(label + " length");
      if (length < 0) {
        throw corrupted(label + " length is negative");
      }
      requireWithin(options.stringOctets(), length);
      try {
        return StandardCharsets.UTF_8
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(readOctets(length, label)))
            .toString();
      } catch (CharacterCodingException exception) {
        throw corrupted(label + " is not valid UTF-8");
      }
    }

    private byte[] readOctets(int length, String label) {
      requireRemaining(length, label);
      byte[] result = java.util.Arrays.copyOfRange(bytes, offset, offset + length);
      offset += length;
      return result;
    }

    private void requireRemaining(int length, String label) {
      if (length < 0 || length > bytes.length - offset) {
        throw corrupted("Naming store ended while reading " + label);
      }
    }

    private void requireFullyRead() {
      if (offset != bytes.length) {
        throw corrupted("Naming store has trailing octets: " + (bytes.length - offset));
      }
    }
  }
}
