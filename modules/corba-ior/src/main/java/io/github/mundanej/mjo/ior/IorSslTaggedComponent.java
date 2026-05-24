package io.github.mundanej.mjo.ior;

import io.github.mundanej.mjo.cdr.CdrByteOrder;
import io.github.mundanej.mjo.cdr.CdrEncapsulation;
import io.github.mundanej.mjo.cdr.CdrException;
import io.github.mundanej.mjo.cdr.CdrReader;
import io.github.mundanej.mjo.cdr.CdrWriter;
import java.util.Objects;

/** Standard TAG_SSL_SEC_TRANS component payload for TLS-capable IIOP profiles. */
public record IorSslTaggedComponent(int targetSupports, int targetRequires, int port) {

  /** Creates a validated SSL/TLS component payload. */
  public IorSslTaggedComponent {
    IorWire.requireUnsignedShort(targetSupports, "targetSupports");
    IorWire.requireUnsignedShort(targetRequires, "targetRequires");
    IorWire.requireUnsignedShort(port, "ssl port");
  }

  /** Decodes a TAG_SSL_SEC_TRANS component. */
  public static IorSslTaggedComponent fromComponent(TaggedComponent component) {
    Objects.requireNonNull(component, "component");
    if (component.tag() != IorTags.TAG_SSL_SEC_TRANS) {
      throw new IorException(
          IorDiagnosticCodes.INVALID_IIOP_PROFILE,
          "component is not TAG_SSL_SEC_TRANS: " + component.tag());
    }
    try {
      CdrReader reader = CdrEncapsulation.fromBytes(component.componentData()).reader();
      IorSslTaggedComponent decoded =
          new IorSslTaggedComponent(
              reader.readUnsignedShort(), reader.readUnsignedShort(), reader.readUnsignedShort());
      if (reader.remaining() != 0) {
        throw new IorException(
            IorDiagnosticCodes.INVALID_IIOP_PROFILE,
            "TAG_SSL_SEC_TRANS component has trailing octets: " + reader.remaining());
      }
      return decoded;
    } catch (CdrException exception) {
      throw new IorException(
          IorDiagnosticCodes.INVALID_IIOP_PROFILE,
          "TAG_SSL_SEC_TRANS component is malformed: " + exception.getMessage());
    }
  }

  /** Encodes this value as a standard tagged component. */
  public TaggedComponent toComponent() {
    CdrWriter writer = new CdrWriter(CdrByteOrder.BIG_ENDIAN);
    writer.writeOctet(0);
    writer.writeUnsignedShort(targetSupports);
    writer.writeUnsignedShort(targetRequires);
    writer.writeUnsignedShort(port);
    return new TaggedComponent(IorTags.TAG_SSL_SEC_TRANS, writer.toByteArray());
  }
}
