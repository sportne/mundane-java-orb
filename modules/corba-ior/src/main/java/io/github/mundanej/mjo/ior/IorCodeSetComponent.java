package io.github.mundanej.mjo.ior;

import io.github.mundanej.mjo.cdr.CdrByteOrder;
import io.github.mundanej.mjo.cdr.CdrEncapsulation;
import io.github.mundanej.mjo.cdr.CdrException;
import io.github.mundanej.mjo.cdr.CdrReader;
import io.github.mundanej.mjo.cdr.CdrWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Standard TAG_CODE_SETS component payload. */
public record IorCodeSetComponent(
    long nativeCharCodeSet,
    List<Long> conversionCharCodeSets,
    long nativeWcharCodeSet,
    List<Long> conversionWcharCodeSets) {

  /** ISO-8859-1 registry value used by local narrow-string CDR payloads. */
  public static final long ISO_8859_1 = 0x0001_0001L;

  /** UTF-8 registry value accepted for peer-facing narrow strings. */
  public static final long UTF_8 = 0x0501_0001L;

  /** UTF-16 registry value used by local wide-string CDR payloads. */
  public static final long UTF_16 = 0x0001_0109L;

  /** Creates a validated code-set component. */
  public IorCodeSetComponent {
    IorWire.requireUnsignedLong(nativeCharCodeSet, "nativeCharCodeSet");
    conversionCharCodeSets = copyCodeSets(conversionCharCodeSets, "conversionCharCodeSets");
    IorWire.requireUnsignedLong(nativeWcharCodeSet, "nativeWcharCodeSet");
    conversionWcharCodeSets = copyCodeSets(conversionWcharCodeSets, "conversionWcharCodeSets");
  }

  /** Returns the deterministic default component for this implementation. */
  public static IorCodeSetComponent defaults() {
    return new IorCodeSetComponent(ISO_8859_1, List.of(UTF_8), UTF_16, List.of());
  }

  @Override
  public List<Long> conversionCharCodeSets() {
    return List.copyOf(conversionCharCodeSets);
  }

  @Override
  public List<Long> conversionWcharCodeSets() {
    return List.copyOf(conversionWcharCodeSets);
  }

  /** Decodes a TAG_CODE_SETS component. */
  public static IorCodeSetComponent fromComponent(TaggedComponent component) {
    Objects.requireNonNull(component, "component");
    if (component.tag() != IorTags.TAG_CODE_SETS) {
      throw new IorException(
          IorDiagnosticCodes.INVALID_IIOP_PROFILE,
          "component is not TAG_CODE_SETS: " + component.tag());
    }
    try {
      CdrReader reader = CdrEncapsulation.fromBytes(component.componentData()).reader();
      IorCodeSetComponent decoded =
          new IorCodeSetComponent(
              reader.readUnsignedLong(),
              readCodeSetSequence(reader),
              reader.readUnsignedLong(),
              readCodeSetSequence(reader));
      if (reader.remaining() != 0) {
        throw new IorException(
            IorDiagnosticCodes.INVALID_IIOP_PROFILE,
            "TAG_CODE_SETS component has trailing octets: " + reader.remaining());
      }
      return decoded;
    } catch (CdrException exception) {
      throw new IorException(
          IorDiagnosticCodes.INVALID_IIOP_PROFILE,
          "TAG_CODE_SETS component is malformed: " + exception.getMessage());
    }
  }

  /** Encodes this value as a standard tagged component. */
  public TaggedComponent toComponent() {
    CdrWriter writer = new CdrWriter(CdrByteOrder.BIG_ENDIAN);
    writer.writeOctet(0);
    writer.writeUnsignedLong(nativeCharCodeSet);
    writeCodeSetSequence(writer, conversionCharCodeSets);
    writer.writeUnsignedLong(nativeWcharCodeSet);
    writeCodeSetSequence(writer, conversionWcharCodeSets);
    return new TaggedComponent(IorTags.TAG_CODE_SETS, writer.toByteArray());
  }

  private static List<Long> readCodeSetSequence(CdrReader reader) {
    int count = reader.readSequenceLength();
    List<Long> result = new ArrayList<>(count);
    for (int index = 0; index < count; index++) {
      result.add(reader.readUnsignedLong());
    }
    return result;
  }

  private static void writeCodeSetSequence(CdrWriter writer, List<Long> codeSets) {
    writer.writeSequenceLength(codeSets.size());
    for (long codeSet : codeSets) {
      writer.writeUnsignedLong(codeSet);
    }
  }

  private static List<Long> copyCodeSets(List<Long> codeSets, String name) {
    Objects.requireNonNull(codeSets, name);
    for (long codeSet : codeSets) {
      IorWire.requireUnsignedLong(codeSet, name);
    }
    return List.copyOf(codeSets);
  }
}
