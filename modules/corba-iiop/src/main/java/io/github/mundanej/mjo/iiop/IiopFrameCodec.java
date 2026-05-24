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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class IiopFrameCodec {

  private static final int HEADER_LENGTH = 12;
  private static final int LITTLE_ENDIAN_FLAG = 0x01;
  private static final int MORE_FRAGMENTS_FLAG = 0x02;
  private static final int MAGIC_G = 'G';
  private static final int MAGIC_I = 'I';
  private static final int MAGIC_O = 'O';
  private static final int MAGIC_P = 'P';
  private static final int REQUEST_TYPE = 0;
  private static final int REPLY_TYPE = 1;
  private static final int LOCATE_REPLY_TYPE = 4;
  private static final int FRAGMENT_TYPE = 7;

  private IiopFrameCodec() {}

  static GiopMessage readMessage(InputStream input, GiopLimits limits) throws IOException {
    try {
      byte[] frame = readFrame(input, limits);
      if (messageType(frame) == FRAGMENT_TYPE) {
        throw invalid("fragment sequence is missing an initial message");
      }
      if (moreFragments(frame)) {
        frame = assembleFragmentedFrame(input, limits, frame);
      }
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

  private static byte[] assembleFragmentedFrame(
      InputStream input, GiopLimits limits, byte[] initialFrame) throws IOException {
    requireFragmentableInitial(initialFrame);
    long requestId = readRequestId(initialFrame);
    List<byte[]> payloads = new ArrayList<>();
    byte[] initialBody = body(initialFrame);
    payloads.add(initialBody);
    long assembledBodySize = initialBody.length;
    long fragmentCount = 0;
    byte[] fragmentFrame;
    do {
      fragmentFrame = readFrame(input, limits);
      fragmentCount++;
      requireLimit(limits.fragmentCount(), fragmentCount);
      if (messageType(fragmentFrame) != FRAGMENT_TYPE) {
        throw invalid("fragment sequence contains a non-fragment after the initial message");
      }
      if (readRequestId(fragmentFrame) != requestId) {
        throw invalid("fragment request id does not match initial message");
      }
      byte[] fragmentPayload =
          Arrays.copyOfRange(body(fragmentFrame), Integer.BYTES, bodySize(fragmentFrame));
      assembledBodySize = addFragmentSize(assembledBodySize, fragmentPayload.length);
      requireLimit(limits.fragmentBodyOctets(), assembledBodySize);
      requireLimit(limits.bodyOctets(), assembledBodySize);
      payloads.add(fragmentPayload);
    } while (moreFragments(fragmentFrame));
    requireLimit(limits.messageOctets(), HEADER_LENGTH + assembledBodySize);
    return assembleFrame(initialFrame, payloads, assembledBodySize);
  }

  private static byte[] assembleFrame(byte[] initialFrame, List<byte[]> payloads, long bodySize) {
    if (bodySize > Integer.MAX_VALUE) {
      throw invalid("assembled fragment body is too large for local allocation");
    }
    byte[] assembled = Arrays.copyOf(initialFrame, HEADER_LENGTH + Math.toIntExact(bodySize));
    assembled[6] = (byte) ((assembled[6] & 0xff) & ~MORE_FRAGMENTS_FLAG);
    writeBodySize(assembled, Math.toIntExact(bodySize));
    int position = HEADER_LENGTH;
    for (byte[] payload : payloads) {
      System.arraycopy(payload, 0, assembled, position, payload.length);
      position += payload.length;
    }
    return assembled;
  }

  private static void requireFragmentableInitial(byte[] frame) {
    int messageType = messageType(frame);
    if (messageType != REQUEST_TYPE
        && messageType != REPLY_TYPE
        && messageType != LOCATE_REPLY_TYPE) {
      throw invalid("message type cannot be fragmented by this local frame reader");
    }
    readRequestId(frame);
  }

  private static long addFragmentSize(long current, int fragmentLength) {
    try {
      return Math.addExact(current, fragmentLength);
    } catch (ArithmeticException exception) {
      throw invalid("assembled fragment body size overflowed");
    }
  }

  private static long readRequestId(byte[] frame) {
    byte[] body = body(frame);
    if (body.length < Integer.BYTES) {
      throw invalid("fragmented message body is missing the request id");
    }
    int first = body[0] & 0xff;
    int second = body[1] & 0xff;
    int third = body[2] & 0xff;
    int fourth = body[3] & 0xff;
    if (littleEndian(frame)) {
      return ((long) fourth << 24) | ((long) third << 16) | ((long) second << 8) | first;
    }
    return ((long) first << 24) | ((long) second << 16) | ((long) third << 8) | fourth;
  }

  private static byte[] body(byte[] frame) {
    return Arrays.copyOfRange(frame, HEADER_LENGTH, frame.length);
  }

  private static int bodySize(byte[] frame) {
    return frame.length - HEADER_LENGTH;
  }

  private static boolean moreFragments(byte[] frame) {
    return ((frame[6] & 0xff) & MORE_FRAGMENTS_FLAG) != 0;
  }

  private static int messageType(byte[] frame) {
    return frame[7] & 0xff;
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
    boolean littleEndian = littleEndian(header);
    int first = header[8] & 0xff;
    int second = header[9] & 0xff;
    int third = header[10] & 0xff;
    int fourth = header[11] & 0xff;
    if (littleEndian) {
      return ((long) fourth << 24) | ((long) third << 16) | ((long) second << 8) | first;
    }
    return ((long) first << 24) | ((long) second << 16) | ((long) third << 8) | fourth;
  }

  private static void writeBodySize(byte[] header, int bodySize) {
    if (littleEndian(header)) {
      header[8] = (byte) bodySize;
      header[9] = (byte) (bodySize >>> 8);
      header[10] = (byte) (bodySize >>> 16);
      header[11] = (byte) (bodySize >>> 24);
      return;
    }
    header[8] = (byte) (bodySize >>> 24);
    header[9] = (byte) (bodySize >>> 16);
    header[10] = (byte) (bodySize >>> 8);
    header[11] = (byte) bodySize;
  }

  private static boolean littleEndian(byte[] header) {
    return ((header[6] & 0xff) & LITTLE_ENDIAN_FLAG) != 0;
  }

  private static void requireLimit(BoundedLimit limit, long observedValue) {
    limit.check(observedValue).map(LimitViolation::message).ifPresent(IiopFrameCodec::throwLimit);
  }

  private static void throwLimit(String message) {
    throw new IiopException(IiopDiagnosticCodes.FRAME_LIMIT, message);
  }

  private static GiopException invalid(String message) {
    return new GiopException(GiopDiagnosticCodes.INVALID_BODY, message);
  }

  private static DiagnosticCode mapGiopCode(GiopException exception) {
    if (GiopDiagnosticCodes.LIMIT_EXCEEDED.equals(exception.code())
        || GiopDiagnosticCodes.MESSAGE_SIZE_MISMATCH.equals(exception.code())) {
      return IiopDiagnosticCodes.FRAME_LIMIT;
    }
    return IiopDiagnosticCodes.UNSUPPORTED_MESSAGE;
  }
}
