package io.github.mundanej.mjo.giop;

import io.github.mundanej.mjo.cdr.CdrByteOrder;
import io.github.mundanej.mjo.cdr.CdrEncapsulation;
import io.github.mundanej.mjo.cdr.CdrReader;
import io.github.mundanej.mjo.cdr.CdrWriter;
import io.github.mundanej.mjo.ior.IorCodeSetComponent;

/** GIOP code-set service context payload. */
public record GiopCodeSetContext(long charCodeSet, long wcharCodeSet) {

  /** Standard service context id for code-set context data. */
  public static final long SERVICE_CONTEXT_ID = 1L;

  /** Creates a validated code-set context. */
  public GiopCodeSetContext {
    validateCodeSet(charCodeSet, "charCodeSet");
    validateCodeSet(wcharCodeSet, "wcharCodeSet");
  }

  /** Returns the deterministic local default code-set context. */
  public static GiopCodeSetContext defaults() {
    return new GiopCodeSetContext(IorCodeSetComponent.ISO_8859_1, IorCodeSetComponent.UTF_16);
  }

  /** Encodes this value as a service context. */
  public GiopServiceContext toServiceContext() {
    CdrWriter writer = new CdrWriter(CdrByteOrder.BIG_ENDIAN);
    writer.writeOctet(0);
    writer.writeUnsignedLong(charCodeSet);
    writer.writeUnsignedLong(wcharCodeSet);
    return new GiopServiceContext(SERVICE_CONTEXT_ID, writer.toByteArray());
  }

  /** Decodes a service context value. */
  public static GiopCodeSetContext fromServiceContext(GiopServiceContext context) {
    if (context.contextId() != SERVICE_CONTEXT_ID) {
      throw new GiopException(
          GiopDiagnosticCodes.INVALID_BODY,
          "service context is not a code-set context: " + context.contextId());
    }
    CdrReader reader = CdrEncapsulation.fromBytes(context.contextData()).reader();
    GiopCodeSetContext decoded =
        new GiopCodeSetContext(reader.readUnsignedLong(), reader.readUnsignedLong());
    if (reader.remaining() != 0) {
      throw new GiopException(
          GiopDiagnosticCodes.INVALID_BODY,
          "code-set service context has trailing octets: " + reader.remaining());
    }
    return decoded;
  }

  private static void validateCodeSet(long value, String name) {
    boolean supported =
        value == IorCodeSetComponent.ISO_8859_1
            || value == IorCodeSetComponent.UTF_8
            || value == IorCodeSetComponent.UTF_16;
    if (!supported) {
      throw new GiopException(
          GiopDiagnosticCodes.UNSUPPORTED_BODY, "Unsupported " + name + ": " + value);
    }
  }
}
