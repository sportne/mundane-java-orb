package io.github.mundanej.mjo.orb;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Versioned durable object key carried inside persistent object references. */
public final class DurableObjectKey {

  private static final byte[] MAGIC = {'M', 'J', 'O', 'K'};
  private static final int VERSION = 1;
  private static final int MAX_FLAGS = 0xFF;
  private static final int MAX_ENCODED_OCTETS = 65_536;
  private static final int MAX_ORB_ID_OCTETS = 128;
  private static final int MAX_POA_COMPONENTS = 32;
  private static final int MAX_POA_COMPONENT_OCTETS = 128;
  private static final int MAX_OBJECT_ID_OCTETS = 4_096;

  private final String orbId;
  private final List<String> poaPath;
  private final byte[] objectId;
  private final int flags;

  /** Creates a durable object key value. */
  public DurableObjectKey(String orbId, List<String> poaPath, byte[] objectId, int flags) {
    this.orbId = OrbIdentity.requireIdentifier(orbId, "orbId", MAX_ORB_ID_OCTETS);
    this.poaPath = validatePath(poaPath);
    this.objectId = validateObjectId(objectId);
    this.flags = requireFlags(flags);
  }

  /** Creates a durable object key from a POA path such as {@code /RootPOA/child}. */
  public static DurableObjectKey fromPoaPath(
      String orbId, String poaPath, byte[] objectId, int flags) {
    Objects.requireNonNull(poaPath, "poaPath");
    if (!poaPath.startsWith("/")) {
      throw new IllegalArgumentException("poaPath must start with /");
    }
    String[] rawComponents = poaPath.substring(1).split("/", -1);
    List<String> components = new ArrayList<>(rawComponents.length);
    for (String component : rawComponents) {
      components.add(component);
    }
    return new DurableObjectKey(orbId, components, objectId, flags);
  }

  /** Returns true when the bytes start with the durable key magic prefix. */
  public static boolean hasDurablePrefix(byte[] encoded) {
    Objects.requireNonNull(encoded, "encoded");
    if (encoded.length < MAGIC.length) {
      return false;
    }
    for (int index = 0; index < MAGIC.length; index++) {
      if (encoded[index] != MAGIC[index]) {
        return false;
      }
    }
    return true;
  }

  /** Decodes a bounded durable object key. */
  public static DurableObjectKey decode(byte[] encoded) {
    Objects.requireNonNull(encoded, "encoded");
    if (encoded.length > MAX_ENCODED_OCTETS) {
      throw new IllegalArgumentException("durable object key exceeds " + MAX_ENCODED_OCTETS);
    }
    Reader reader = new Reader(encoded);
    reader.requireMagic();
    int version = reader.readUnsignedByte("version");
    if (version != VERSION) {
      throw new IllegalArgumentException("unsupported durable object key version: " + version);
    }
    int flags = reader.readUnsignedByte("flags");
    String orbId =
        reader.readAscii(reader.readUnsignedShort("orbId length"), "orbId", MAX_ORB_ID_OCTETS);
    int componentCount = reader.readUnsignedShort("POA path component count");
    if (componentCount == 0 || componentCount > MAX_POA_COMPONENTS) {
      throw new IllegalArgumentException("invalid POA path component count: " + componentCount);
    }
    List<String> components = new ArrayList<>(componentCount);
    for (int index = 0; index < componentCount; index++) {
      components.add(
          reader.readAscii(
              reader.readUnsignedShort("POA component length"),
              "POA component",
              MAX_POA_COMPONENT_OCTETS));
    }
    int objectIdLength = reader.readUnsignedShort("object id length");
    byte[] objectId = reader.readOctets(objectIdLength, "object id");
    reader.requireFullyRead();
    return new DurableObjectKey(orbId, components, objectId, flags);
  }

  /** Returns the configured durable ORB id. */
  public String orbId() {
    return orbId;
  }

  /** Returns the stable POA path components. */
  public List<String> poaPath() {
    return List.copyOf(poaPath);
  }

  /** Returns the stable POA path with a leading slash. */
  public String poaPathString() {
    return "/" + String.join("/", poaPath);
  }

  /** Returns a defensive copy of the object-id octets. */
  public byte[] objectId() {
    return Arrays.copyOf(objectId, objectId.length);
  }

  /** Returns durable key flags. */
  public int flags() {
    return flags;
  }

  /** Encodes this key as a bounded versioned binary value. */
  public byte[] encode() {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    output.writeBytes(MAGIC);
    output.write(VERSION);
    output.write(flags);
    writeAscii(output, orbId);
    writeUnsignedShort(output, poaPath.size());
    for (String component : poaPath) {
      writeAscii(output, component);
    }
    writeUnsignedShort(output, objectId.length);
    output.writeBytes(objectId);
    byte[] result = output.toByteArray();
    if (result.length > MAX_ENCODED_OCTETS) {
      throw new IllegalArgumentException("durable object key exceeds " + MAX_ENCODED_OCTETS);
    }
    return result;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof DurableObjectKey that
        && orbId.equals(that.orbId)
        && poaPath.equals(that.poaPath)
        && Arrays.equals(objectId, that.objectId)
        && flags == that.flags;
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(orbId, poaPath, flags);
    return 31 * result + Arrays.hashCode(objectId);
  }

  @Override
  public String toString() {
    return "DurableObjectKey[orbId="
        + orbId
        + ", poaPath="
        + poaPathString()
        + ", objectIdOctets="
        + objectId.length
        + ", flags="
        + flags
        + "]";
  }

  private static List<String> validatePath(List<String> path) {
    Objects.requireNonNull(path, "poaPath");
    if (path.isEmpty() || path.size() > MAX_POA_COMPONENTS) {
      throw new IllegalArgumentException("POA path must contain 1.." + MAX_POA_COMPONENTS);
    }
    List<String> result = new ArrayList<>(path.size());
    for (String component : path) {
      String checked =
          OrbIdentity.requireIdentifier(component, "POA path component", MAX_POA_COMPONENT_OCTETS);
      if (".".equals(checked) || "..".equals(checked)) {
        throw new IllegalArgumentException("POA path component must not be traversal: " + checked);
      }
      result.add(checked);
    }
    return List.copyOf(result);
  }

  private static byte[] validateObjectId(byte[] value) {
    Objects.requireNonNull(value, "objectId");
    if (value.length == 0 || value.length > MAX_OBJECT_ID_OCTETS) {
      throw new IllegalArgumentException("objectId must contain 1.." + MAX_OBJECT_ID_OCTETS);
    }
    return Arrays.copyOf(value, value.length);
  }

  private static int requireFlags(int value) {
    if (value < 0 || value > MAX_FLAGS) {
      throw new IllegalArgumentException("flags must fit in one octet: " + value);
    }
    return value;
  }

  private static void writeAscii(ByteArrayOutputStream output, String value) {
    byte[] octets = value.getBytes(StandardCharsets.US_ASCII);
    writeUnsignedShort(output, octets.length);
    output.writeBytes(octets);
  }

  private static void writeUnsignedShort(ByteArrayOutputStream output, int value) {
    if (value < 0 || value > 0xFFFF) {
      throw new IllegalArgumentException("value must fit in unsigned short: " + value);
    }
    output.write((value >>> 8) & 0xFF);
    output.write(value & 0xFF);
  }

  private static final class Reader {

    private final byte[] encoded;
    private int offset;

    private Reader(byte[] encoded) {
      this.encoded = encoded;
    }

    private void requireMagic() {
      byte[] actual = readOctets(MAGIC.length, "magic");
      if (!Arrays.equals(MAGIC, actual)) {
        throw new IllegalArgumentException("durable object key magic is invalid");
      }
    }

    private int readUnsignedByte(String label) {
      requireRemaining(1, label);
      return encoded[offset++] & 0xFF;
    }

    private int readUnsignedShort(String label) {
      requireRemaining(2, label);
      int value = ((encoded[offset] & 0xFF) << 8) | (encoded[offset + 1] & 0xFF);
      offset += 2;
      return value;
    }

    private String readAscii(int length, String label, int maxOctets) {
      byte[] octets = readOctets(length, label);
      String value = new String(octets, StandardCharsets.US_ASCII);
      return OrbIdentity.requireIdentifier(value, label, maxOctets);
    }

    private byte[] readOctets(int length, String label) {
      if (length < 0) {
        throw new IllegalArgumentException(label + " length must not be negative");
      }
      requireRemaining(length, label);
      byte[] octets = Arrays.copyOfRange(encoded, offset, offset + length);
      offset += length;
      return octets;
    }

    private void requireRemaining(int length, String label) {
      if (encoded.length - offset < length) {
        throw new IllegalArgumentException("durable object key is truncated reading " + label);
      }
    }

    private void requireFullyRead() {
      if (offset != encoded.length) {
        throw new IllegalArgumentException(
            "durable object key has trailing octets: " + (encoded.length - offset));
      }
    }
  }
}
