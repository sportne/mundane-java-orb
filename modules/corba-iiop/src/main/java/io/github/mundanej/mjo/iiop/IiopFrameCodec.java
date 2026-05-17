package io.github.mundanej.mjo.iiop;

import io.github.mundanej.mjo.common.BoundedLimit;
import io.github.mundanej.mjo.common.DiagnosticCode;
import io.github.mundanej.mjo.common.LimitViolation;
import io.github.mundanej.mjo.giop.GiopDiagnosticCodes;
import io.github.mundanej.mjo.giop.GiopException;
import io.github.mundanej.mjo.giop.GiopLimits;
import io.github.mundanej.mjo.giop.GiopMessage;
import io.github.mundanej.mjo.giop.GiopMessageReader;
import io.github.mundanej.mjo.giop.GiopMessageWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.util.Arrays;

final class IiopFrameCodec {

  private static final int HEADER_LENGTH = 12;
  private static final int LITTLE_ENDIAN_FLAG = 0x01;
  private static final int MAGIC_G = 'G';
  private static final int MAGIC_I = 'I';
  private static final int MAGIC_O = 'O';
  private static final int MAGIC_P = 'P';

  private IiopFrameCodec() {}

  static GiopMessage readMessage(InputStream input, GiopLimits limits) throws IOException {
    byte[] frame = readFrame(input, limits);
    try {
      return new GiopMessageReader(limits).read(frame);
    } catch (GiopException exception) {
      throw new IiopException(
          mapGiopCode(exception),
          "GIOP frame is not supported by the local IIOP TCP slice: " + exception.getMessage(),
          exception);
    }
  }

  static void writeMessage(OutputStream output, GiopMessage message, GiopLimits limits)
      throws IOException {
    byte[] bytes = new GiopMessageWriter(limits).write(message);
    output.write(bytes);
    output.flush();
  }

  private static byte[] readFrame(InputStream input, GiopLimits limits) throws IOException {
    byte[] header = readHeader(input);
    requireMagic(header);
    long bodySize = readBodySize(header);
    requireLimit(limits.bodyOctets(), bodySize);
    requireLimit(limits.messageOctets(), HEADER_LENGTH + bodySize);
    if (bodySize > Integer.MAX_VALUE) {
      throw new IiopException(
          IiopDiagnosticCodes.FRAME_LIMIT, "GIOP body is too large for local allocation");
    }
    byte[] frame = Arrays.copyOf(header, HEADER_LENGTH + Math.toIntExact(bodySize));
    readFully(input, frame, HEADER_LENGTH, Math.toIntExact(bodySize), true);
    return frame;
  }

  private static byte[] readHeader(InputStream input) throws IOException {
    byte[] header = new byte[HEADER_LENGTH];
    readFully(input, header, 0, HEADER_LENGTH, false);
    return header;
  }

  private static void readFully(
      InputStream input, byte[] target, int offset, int byteCount, boolean body)
      throws IOException {
    int position = offset;
    int end = offset + byteCount;
    while (position < end) {
      int count;
      try {
        count = input.read(target, position, end - position);
      } catch (SocketTimeoutException exception) {
        throw new IiopException(
            IiopDiagnosticCodes.READ_TIMEOUT, "Timed out while reading IIOP frame", exception);
      }
      if (count < 0) {
        throw new IiopException(
            IiopDiagnosticCodes.EOF,
            (body ? "GIOP body" : "GIOP header") + " ended before a full frame was available");
      }
      position += count;
    }
  }

  private static void requireMagic(byte[] header) {
    if ((header[0] & 0xff) != MAGIC_G
        || (header[1] & 0xff) != MAGIC_I
        || (header[2] & 0xff) != MAGIC_O
        || (header[3] & 0xff) != MAGIC_P) {
      throw new IiopException(IiopDiagnosticCodes.UNSUPPORTED_MESSAGE, "Invalid GIOP magic");
    }
  }

  private static long readBodySize(byte[] header) {
    boolean littleEndian = ((header[6] & 0xff) & LITTLE_ENDIAN_FLAG) != 0;
    int first = header[8] & 0xff;
    int second = header[9] & 0xff;
    int third = header[10] & 0xff;
    int fourth = header[11] & 0xff;
    if (littleEndian) {
      return ((long) fourth << 24) | ((long) third << 16) | ((long) second << 8) | first;
    }
    return ((long) first << 24) | ((long) second << 16) | ((long) third << 8) | fourth;
  }

  private static void requireLimit(BoundedLimit limit, long observedValue) {
    limit.check(observedValue).map(LimitViolation::message).ifPresent(IiopFrameCodec::throwLimit);
  }

  private static void throwLimit(String message) {
    throw new IiopException(IiopDiagnosticCodes.FRAME_LIMIT, message);
  }

  private static DiagnosticCode mapGiopCode(GiopException exception) {
    if (GiopDiagnosticCodes.LIMIT_EXCEEDED.equals(exception.code())
        || GiopDiagnosticCodes.MESSAGE_SIZE_MISMATCH.equals(exception.code())) {
      return IiopDiagnosticCodes.FRAME_LIMIT;
    }
    return IiopDiagnosticCodes.UNSUPPORTED_MESSAGE;
  }
}
