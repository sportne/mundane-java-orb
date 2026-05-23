package io.github.mundanej.mjo.ior;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjo.common.BoundedLimit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit and negative tests for corbaloc and corbaname object URLs. */
@Tag("unit")
final class ObjectUrlTest {

  @Test
  void parsesDefaultAndExplicitIiopCorbalocUrls() {
    CorbalocUrl defaultUrl = CorbalocUrl.parse("corbaloc::555xyz.com/Prod/TradingService");
    CorbalocAddress defaultAddress = defaultUrl.addresses().get(0);
    CorbalocUrl explicitUrl =
        CorbalocUrl.parse("corbaloc:iiop:1.2@192.168.14.25:555/Name%20Service");
    CorbalocAddress explicitAddress = explicitUrl.addresses().get(0);

    assertEquals(CorbalocAddress.Kind.IIOP, defaultAddress.kind());
    assertEquals("iiop", defaultAddress.protocol());
    assertEquals(IiopVersion.V1_0, defaultAddress.version().orElseThrow());
    assertEquals("555xyz.com", defaultAddress.host().orElseThrow());
    assertEquals(2809, defaultAddress.port().orElseThrow());
    assertEquals("Prod/TradingService", defaultUrl.keyString());
    assertArrayEquals(ascii("Prod/TradingService"), defaultUrl.objectKey().octets());
    assertEquals(IiopVersion.V1_2, explicitAddress.version().orElseThrow());
    assertEquals("192.168.14.25", explicitAddress.host().orElseThrow());
    assertEquals(555, explicitAddress.port().orElseThrow());
    assertArrayEquals(ascii("Name Service"), explicitUrl.objectKey().octets());
  }

  @Test
  void parsesMultipleIpv6RirAndFutureCorbalocUrls() {
    CorbalocUrl multiple =
        CorbalocUrl.parse("corbaloc::[1080::8:800:200C:417A]:88,atm:E.164:358.400/x");
    CorbalocUrl rir = CorbalocUrl.parse("corbaloc:rir:/NameService");

    assertEquals(2, multiple.addresses().size());
    assertEquals("[1080::8:800:200C:417A]", multiple.addresses().get(0).host().orElseThrow());
    assertEquals(88, multiple.addresses().get(0).port().orElseThrow());
    assertEquals(CorbalocAddress.Kind.FUTURE, multiple.addresses().get(1).kind());
    assertEquals("atm", multiple.addresses().get(1).protocol());
    assertEquals(
        "E.164:358.400", multiple.addresses().get(1).protocolSpecificAddress().orElseThrow());
    assertEquals(CorbalocAddress.Kind.RIR, rir.addresses().get(0).kind());
    assertFalse(rir.addresses().get(0).host().isPresent());
  }

  @Test
  void parsesCorbanameUrlsAsCorbalocPlusName() {
    CorbanameUrl url = CorbanameUrl.parse("corbaname::555objs.com#a/string/path%20to/obj");
    CorbanameUrl rir = CorbanameUrl.parse("corbaname:rir:#a/local/obj");

    assertEquals("555objs.com", url.location().addresses().get(0).host().orElseThrow());
    assertEquals("", url.location().keyString());
    assertEquals("a/string/path to/obj", url.stringName());
    assertEquals(CorbalocAddress.Kind.RIR, rir.location().addresses().get(0).kind());
    assertEquals("a/local/obj", rir.stringName());
  }

  @Test
  void parsesBoundaryObjectUrlsAndAddressValueObjects() {
    CorbalocUrl noSlash = CorbalocUrl.parse("CORBALOC::h");
    CorbalocUrl escaped = CorbalocUrl.parse("corbaloc::h/%2F%00%7f");
    CorbanameUrl noFragment = CorbanameUrl.parse("corbaname::h");
    CorbalocAddress future = CorbalocAddress.future("proto", "addr");

    assertEquals("", noSlash.keyString());
    assertArrayEquals(bytes(), noSlash.objectKey().octets());
    assertArrayEquals(bytes('/', 0x00, 0x7F), escaped.objectKey().octets());
    assertEquals("", noFragment.stringName());
    assertEquals("", noFragment.location().keyString());
    assertEquals(CorbalocAddress.Kind.FUTURE, future.kind());
    assertEquals("proto", future.protocol());
    assertEquals("addr", future.protocolSpecificAddress().orElseThrow());
    assertTrue(future.toString().contains("proto:addr"));
    assertIorCode(IorDiagnosticCodes.INVALID_OBJECT_URL, () -> CorbalocAddress.future(" ", "x"));
    assertThrows(NullPointerException.class, () -> CorbalocAddress.iiop(null, "h", 1));
    assertThrows(NullPointerException.class, () -> CorbalocAddress.iiop(IiopVersion.V1_0, null, 1));
  }

  @Test
  void reportsInvalidObjectUrlsAndBounds() {
    IorLimits strict =
        new IorLimits(
            new BoundedLimit("test-string", 32),
            new BoundedLimit("test-sequence", 32),
            new BoundedLimit("test-encapsulation", 32),
            new BoundedLimit("test-profile-count", 1),
            new BoundedLimit("test-profile-data", 32),
            new BoundedLimit("test-component-count", 1),
            new BoundedLimit("test-component-data", 32),
            new BoundedLimit("test-object-key", 1),
            new BoundedLimit("test-url", 10));

    assertIorCode(IorDiagnosticCodes.INVALID_OBJECT_URL, () -> CorbalocUrl.parse("http://x"));
    assertIorCode(IorDiagnosticCodes.INVALID_OBJECT_URL, () -> CorbalocUrl.parse("corbaloc:"));
    assertIorCode(
        IorDiagnosticCodes.INVALID_OBJECT_URL, () -> CorbalocUrl.parse("corbaloc::h,,atm:x/k"));
    assertIorCode(
        IorDiagnosticCodes.INVALID_OBJECT_URL, () -> CorbalocUrl.parse("corbaloc:rir:,:h/k"));
    assertIorCode(
        IorDiagnosticCodes.INVALID_OBJECT_URL, () -> CorbalocUrl.parse("corbaloc:iiop:1@h/k"));
    assertIorCode(
        IorDiagnosticCodes.INVALID_OBJECT_URL, () -> CorbalocUrl.parse("corbaloc:iiop:1.x@h/k"));
    assertIorCode(
        IorDiagnosticCodes.INVALID_OBJECT_URL, () -> CorbalocUrl.parse("corbaloc:iiop:.2@h/k"));
    assertIorCode(IorDiagnosticCodes.INVALID_PORT, () -> CorbalocUrl.parse("corbaloc::h:99999/k"));
    assertIorCode(IorDiagnosticCodes.INVALID_PORT, () -> CorbalocUrl.parse("corbaloc::h:/k"));
    assertIorCode(IorDiagnosticCodes.INVALID_PORT, () -> CorbalocUrl.parse("corbaloc::h:abc/k"));
    assertIorCode(
        IorDiagnosticCodes.INVALID_OBJECT_URL, () -> CorbalocUrl.parse("corbaloc::[1080::1/k"));
    assertIorCode(
        IorDiagnosticCodes.INVALID_OBJECT_URL, () -> CorbalocUrl.parse("corbaloc::[1080::1]x/k"));
    assertIorCode(IorDiagnosticCodes.INVALID_OBJECT_URL, () -> CorbalocUrl.parse("corbaloc::h/%"));
    assertIorCode(
        IorDiagnosticCodes.INVALID_OBJECT_URL, () -> CorbalocUrl.parse("corbaloc::h/%GG"));
    assertIorCode(
        IorDiagnosticCodes.INVALID_OBJECT_URL, () -> CorbanameUrl.parse("corbaname::h#%"));
    assertIorCode(
        IorDiagnosticCodes.INVALID_OBJECT_URL, () -> CorbanameUrl.parse("corbaname::h#caf\u00E9"));
    assertEquals(
        "", CorbalocUrl.parse("corbaloc::", strict).addresses().getFirst().host().orElseThrow());
    assertIorCode(
        IorDiagnosticCodes.LENGTH_LIMIT_EXCEEDED,
        () -> CorbalocUrl.parse("corbaloc::h/key", strict));
  }

  @Test
  void constructorRejectsEmptyAddresses() {
    assertThrows(
        IorException.class, () -> new CorbalocUrl(java.util.List.of(), "", ObjectKey.empty()));
    assertTrue(CorbalocAddress.rir().toString().contains("rir"));
    assertTrue(CorbalocAddress.iiop(IiopVersion.V1_1, "h", 2).toString().contains("iiop"));
  }

  @Test
  @Tag("security")
  void hostileObjectUrlsFailWithBoundedDiagnostics() {
    IorLimits strict =
        new IorLimits(
            new BoundedLimit("test-string", 16),
            new BoundedLimit("test-sequence", 16),
            new BoundedLimit("test-encapsulation", 16),
            new BoundedLimit("test-profile-count", 1),
            new BoundedLimit("test-profile-data", 16),
            new BoundedLimit("test-component-count", 1),
            new BoundedLimit("test-component-data", 16),
            new BoundedLimit("test-object-key", 4),
            new BoundedLimit("test-url", 18));

    String[] malformedUrls = {
      "http://x",
      "corbaloc:",
      "corbaloc::h,,atm:x/k",
      "corbaloc::[1080::1/k",
      "corbaloc::host/%GG",
      "corbaname::host#bad\u00E9"
    };
    for (String value : malformedUrls) {
      assertIorCode(IorDiagnosticCodes.INVALID_OBJECT_URL, () -> parseObjectUrl(value));
    }
    assertIorCode(
        IorDiagnosticCodes.INVALID_PORT, () -> CorbalocUrl.parse("corbaloc::host:65536/key"));
    assertIorCode(
        IorDiagnosticCodes.LENGTH_LIMIT_EXCEEDED,
        () -> CorbanameUrl.parse("corbaname::host.example#name", strict));
    assertIorCode(
        IorDiagnosticCodes.LENGTH_LIMIT_EXCEEDED,
        () -> CorbalocUrl.parse("corbaloc::host/12345", strict));
  }

  @Test
  @Tag("security")
  void boundedObjectUrlSmokeRemainsDeterministicAcrossRepeatedParses() {
    for (int iteration = 0; iteration < 128; iteration++) {
      CorbalocUrl location = CorbalocUrl.parse("corbaloc:iiop:1.2@127.0.0.1:2809/NameService");
      CorbanameUrl name = CorbanameUrl.parse("corbaname:rir:#root/context");

      assertEquals("127.0.0.1", location.addresses().getFirst().host().orElseThrow());
      assertArrayEquals(ascii("NameService"), location.objectKey().octets());
      assertEquals("root/context", name.stringName());
      assertEquals(CorbalocAddress.Kind.RIR, name.location().addresses().getFirst().kind());
    }
  }

  private static void assertIorCode(Object expectedCode, ThrowingRunnable runnable) {
    IorException exception = assertThrows(IorException.class, runnable::run);
    assertEquals(expectedCode, exception.code());
  }

  private static void parseObjectUrl(String value) {
    if (value.startsWith("corbaname:")) {
      CorbanameUrl.parse(value);
    } else {
      CorbalocUrl.parse(value);
    }
  }

  private static byte[] ascii(String value) {
    byte[] bytes = new byte[value.length()];
    for (int index = 0; index < value.length(); index++) {
      bytes[index] = (byte) value.charAt(index);
    }
    return bytes;
  }

  private static byte[] bytes(int... values) {
    byte[] bytes = new byte[values.length];
    for (int index = 0; index < values.length; index++) {
      bytes[index] = (byte) values[index];
    }
    return bytes;
  }

  @FunctionalInterface
  private interface ThrowingRunnable {

    void run();
  }
}
