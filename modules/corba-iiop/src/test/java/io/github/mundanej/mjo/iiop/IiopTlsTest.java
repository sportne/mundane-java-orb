package io.github.mundanej.mjo.iiop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mundanej.mjo.cdr.CdrReader;
import io.github.mundanej.mjo.cdr.CdrWriter;
import io.github.mundanej.mjo.giop.GiopHeader;
import io.github.mundanej.mjo.giop.GiopLimits;
import io.github.mundanej.mjo.giop.GiopMessageType;
import io.github.mundanej.mjo.giop.GiopReply;
import io.github.mundanej.mjo.giop.GiopReplyStatus;
import io.github.mundanej.mjo.giop.GiopRequest;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** TLS and mTLS loopback tests for endpoint-local IIOP configuration. */
@Tag("unit")
final class IiopTlsTest {

  private static final char[] PASSWORD = "changeit".toCharArray();

  @Test
  void tlsLoopbackHelloRequestReplySucceedsWithTrustedServerCertificate() {
    IiopOptions serverOptions = tlsOptions(IiopTlsOptions.tls(serverContext()));
    IiopOptions clientOptions = tlsOptions(IiopTlsOptions.tls(clientTrustingServerContext()));

    try (IiopServer server =
        IiopServer.bind(IiopEndpoint.loopback(0), serverOptions, IiopTlsTest::handleHello)) {
      try (IiopClient client = IiopClient.connect(server.endpoint(), clientOptions)) {
        GiopReply reply = client.invoke(helloRequest(21, "Tess"));

        assertEquals(GiopReplyStatus.NO_EXCEPTION, reply.replyStatus());
        assertEquals("Hello, Tess", CdrReader.bigEndian(reply.body()).readString());
      }
    }
  }

  @Test
  void mutualTlsLoopbackHelloRequestReplySucceedsWithTrustedClientCertificate() {
    IiopOptions serverOptions = tlsOptions(IiopTlsOptions.mutualTls(serverTrustingClientContext()));
    IiopOptions clientOptions =
        tlsOptions(IiopTlsOptions.mutualTls(clientWithCertificateContext()));

    try (IiopServer server =
        IiopServer.bind(IiopEndpoint.loopback(0), serverOptions, IiopTlsTest::handleHello)) {
      try (IiopClient client = IiopClient.connect(server.endpoint(), clientOptions)) {
        GiopReply reply = client.invoke(helloRequest(22, "Mina"));

        assertEquals(GiopReplyStatus.NO_EXCEPTION, reply.replyStatus());
        assertEquals("Hello, Mina", CdrReader.bigEndian(reply.body()).readString());
      }
    }
  }

  @Test
  void untrustedServerCertificateFailsWithTlsDiagnostic() {
    IiopOptions serverOptions = tlsOptions(IiopTlsOptions.tls(serverContext()));
    IiopOptions clientOptions = tlsOptions(IiopTlsOptions.tls(clientTrustingClientOnlyContext()));

    try (IiopServer server =
        IiopServer.bind(IiopEndpoint.loopback(0), serverOptions, IiopTlsTest::handleHello)) {
      assertIiopCode(
          IiopDiagnosticCodes.TLS_HANDSHAKE_FAILURE,
          () -> IiopClient.connect(server.endpoint(), clientOptions));
    }
  }

  @Test
  void mutualTlsServerRejectsClientWithoutCertificate() {
    IiopOptions serverOptions = tlsOptions(IiopTlsOptions.mutualTls(serverTrustingClientContext()));
    IiopOptions clientOptions = tlsOptions(IiopTlsOptions.tls(clientTrustingServerContext()));

    try (IiopServer server =
        IiopServer.bind(IiopEndpoint.loopback(0), serverOptions, IiopTlsTest::handleHello)) {
      try (IiopClient client = IiopClient.connect(server.endpoint(), clientOptions)) {
        assertIiopCode(
            IiopDiagnosticCodes.TLS_HANDSHAKE_FAILURE,
            () -> client.invoke(helloRequest(23, "NoCert")));
        assertIiopCode(
            IiopDiagnosticCodes.LIFECYCLE, () -> client.invoke(helloRequest(24, "NoCert")));
      }
    }
  }

  @Test
  void tlsClientAndServerShutdownRemainIdempotent() {
    IiopOptions serverOptions = tlsOptions(IiopTlsOptions.tls(serverContext()));
    IiopOptions clientOptions = tlsOptions(IiopTlsOptions.tls(clientTrustingServerContext()));
    IiopServer server =
        IiopServer.bind(IiopEndpoint.loopback(0), serverOptions, IiopTlsTest::handleHello);
    IiopClient client = IiopClient.connect(server.endpoint(), clientOptions);

    client.close();
    client.close();
    assertIiopCode(IiopDiagnosticCodes.LIFECYCLE, () -> client.invoke(helloRequest(25, "Nia")));

    server.close();
    server.close();
  }

  @Test
  void tlsOptionsAreEndpointLocalAndValidated() {
    SSLContext context = serverContext();

    assertEquals(IiopTlsOptions.Mode.DISABLED, IiopTlsOptions.disabled().mode());
    assertThrows(NullPointerException.class, () -> IiopTlsOptions.tls(null));
    assertIiopCode(
        IiopDiagnosticCodes.INVALID_CONFIGURATION,
        () -> new IiopTlsOptions(IiopTlsOptions.Mode.DISABLED, context, List.of(), List.of()));
    assertIiopCode(
        IiopDiagnosticCodes.INVALID_CONFIGURATION,
        () -> new IiopTlsOptions(IiopTlsOptions.Mode.TLS, context, List.of(" "), List.of()));
  }

  private static IiopOptions tlsOptions(IiopTlsOptions tlsOptions) {
    return new IiopOptions(
        Duration.ofSeconds(2), Duration.ofSeconds(2), 4, 4, GiopLimits.defaults(), tlsOptions);
  }

  private static GiopReply handleHello(GiopRequest request) {
    String name = CdrReader.bigEndian(request.body()).readString();
    return new GiopReply(
        GiopHeader.forType(GiopMessageType.REPLY),
        request.requestId(),
        GiopReplyStatus.NO_EXCEPTION,
        List.of(),
        CdrWriter.bigEndian().writeString("Hello, " + name).toByteArray());
  }

  private static GiopRequest helloRequest(long requestId, String name) {
    return new GiopRequest(
        GiopHeader.forType(GiopMessageType.REQUEST),
        requestId,
        3,
        "hello".getBytes(StandardCharsets.US_ASCII),
        "greet",
        List.of(),
        CdrWriter.bigEndian().writeString(name).toByteArray());
  }

  private static SSLContext serverContext() {
    return sslContext(keyManagers(SERVER_KEY, SERVER_CERT), trustManagers());
  }

  private static SSLContext serverTrustingClientContext() {
    return sslContext(keyManagers(SERVER_KEY, SERVER_CERT), trustManagers(CLIENT_CERT));
  }

  private static SSLContext clientTrustingServerContext() {
    return sslContext(null, trustManagers(SERVER_CERT));
  }

  private static SSLContext clientTrustingClientOnlyContext() {
    return sslContext(null, trustManagers(CLIENT_CERT));
  }

  private static SSLContext clientWithCertificateContext() {
    return sslContext(keyManagers(CLIENT_KEY, CLIENT_CERT), trustManagers(SERVER_CERT));
  }

  private static SSLContext sslContext(KeyManager[] keyManagers, TrustManager[] trustManagers) {
    try {
      SSLContext context = SSLContext.getInstance("TLS");
      context.init(keyManagers, trustManagers, new SecureRandom());
      return context;
    } catch (Exception exception) {
      throw new IllegalStateException("Could not create test SSLContext", exception);
    }
  }

  private static KeyManager[] keyManagers(String privateKeyPem, String certificatePem) {
    try {
      KeyStore keyStore = KeyStore.getInstance("PKCS12");
      keyStore.load(null, PASSWORD);
      keyStore.setKeyEntry(
          "key",
          privateKey(privateKeyPem),
          PASSWORD,
          new Certificate[] {certificate(certificatePem)});
      KeyManagerFactory factory =
          KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      factory.init(keyStore, PASSWORD);
      return factory.getKeyManagers();
    } catch (Exception exception) {
      throw new IllegalStateException("Could not create test key managers", exception);
    }
  }

  private static TrustManager[] trustManagers(String... certificatePems) {
    try {
      KeyStore trustStore = KeyStore.getInstance("PKCS12");
      trustStore.load(null, PASSWORD);
      for (int index = 0; index < certificatePems.length; index++) {
        trustStore.setCertificateEntry("cert-" + index, certificate(certificatePems[index]));
      }
      TrustManagerFactory factory =
          TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      factory.init(trustStore);
      return factory.getTrustManagers();
    } catch (Exception exception) {
      throw new IllegalStateException("Could not create test trust managers", exception);
    }
  }

  private static PrivateKey privateKey(String pem) throws Exception {
    byte[] bytes =
        Base64.getMimeDecoder()
            .decode(
                pem.replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", ""));
    return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(bytes));
  }

  private static Certificate certificate(String pem) throws Exception {
    return CertificateFactory.getInstance("X.509")
        .generateCertificate(new ByteArrayInputStream(pem.getBytes(StandardCharsets.US_ASCII)));
  }

  private static void assertIiopCode(Object expectedCode, ThrowingRunnable runnable) {
    IiopException exception = assertThrows(IiopException.class, runnable::run);
    assertEquals(expectedCode, exception.code());
  }

  @FunctionalInterface
  private interface ThrowingRunnable {

    void run() throws Exception;
  }

  private static final String SERVER_KEY =
      """
      -----BEGIN PRIVATE KEY-----
      MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC0MZC73+ShcbtY
      ej6j4C6ahiORTQxostbCVAy71CEBr0A0UQ/Ydwhk6PLKhdUKnk5y4Gj/GNPnGg4L
      d68ZvUxLo9p2xvqB0x6WG+hcYBZOc1LHcM2WByFtYCq6Ivxs2VTOWW/8XHdJgvSX
      ElPk2ejZqG8OQGR9QkvAeRL5mTK75qJVVy4ZgSOGRGZo/XJN6TQDUvIB1bcy7zPB
      qGuxV3xGFuze9ZTVazr3bL/uriEsjPWU78MDs4wcDwT255nxKJY2e0hRzfQ+zTqj
      QZn9bdB8qVV0J7opQmhPdbqlNjFARuGPKKxsmTRFfNhkMwXlVyNqb1l2cSnq393l
      whh4+OAhAgMBAAECggEACvnZ7xe8PXlICCOqHSFStWbmU7t1vgrmuf+UpvxM5mVe
      a9FkPbNCd6MY5u0wBO2Pqb+xpZhFkBYuNNSdfdviAdmBPSrxp2bMvigFB1tzAGRV
      7oAYhcL5bkglUiE/bwHuAIPCJoUbXwbzDyC2q4H6hQeVfDC/7kFTi+3UEhAa3wcb
      OFo0oCVScIfNc7tXTVBRFC2VkngQHDh/lMXbMZjhGH/c2JZoe4+dveCxo9MBwcNm
      Y3kx0w/UypQj1s8TyTVWR/C6G1Nbf+EBEacZWbNgybhuZjWsEARZ4E4bhEmn05ue
      D8BEUM8rp9ZClYa6U0phdl9w82LW0qLYRN+0o+Mj4QKBgQDijQH9fgMZz9mOatMC
      lTaFRwAL6XOWxgRaVjDHUrKy7m7jBDyepl20cKLqjMsGGpyqxZtnjEuLXuG7qLZP
      AnDfArp6WTgtI/dtCE0StJPGeGRqfx53wSXGqNB+BT77bOeStIcS+ChbPIm/B/HX
      AQuorbiqTGBWTaYH0a2dRM/0uwKBgQDLneqsm+FmMemwxrVFnIGI/xpajHqWJZFW
      xeydOO59U32gp7ln8wW/Hr6T8DXbqXayABf+VfQRg6iDme7yKSFuhu1Oew5qCiEo
      PZLcZUcncfqFdxVtItOpDDnE76qBan5+3Mphd0ufo8K4J9eCYrXiUfI9F1qPiym/
      AqOEuIje0wKBgBZIZD+4mzl4th9J5oNEhd5KA3kodxQHWfpAq/+O5MCwrvtpOUUp
      e+H5iiyjuHxbNbmeQhDHPc7xgCZjC7tttAhbTlgmWl4mQXnZ7Sq/1CEbSlUnN6nz
      t3F4vTkH7w/vwD8vvnFhMyb2J15Au8q38ZufqqAlbvxOGtx+BSOI1yNtAoGBAJUY
      COJjMeSW37slhU53jxI1WaK8Anc3Avk67EsjiN4T6IVeyxAmjyqm5H2HAqjZqaQl
      FWySm6jHcvkKGkEPhP9dpR42VROoXycMDyF6t2NWvk5EKFhFjB7YaOSfaYZh6EYU
      fcxogeNfEhGuestqZCxYPLwnAZTiHatTz1A7n10pAoGALcvIqeNdAiKJRCVn9cZn
      nqoxgmyTkgXa6qHcdgr7muhtVIcEg6+dm+KRfNK/EHc2QTBJWqBc5L/nrGdkq04n
      hli8gslOflHSwSrjiWNaYzniVF9d0qRWYxsBU4OULd0xizPuCcxGg/3U/1XlmvkU
      rqi8GGF/cz0RP73Z004XFx8=
      -----END PRIVATE KEY-----
      """;

  private static final String SERVER_CERT =
      """
      -----BEGIN CERTIFICATE-----
      MIIDJTCCAg2gAwIBAgIURgDHGlZq7QFV2W0gFJj4Z7rFOVYwDQYJKoZIhvcNAQEL
      BQAwFDESMBAGA1UEAwwJbG9jYWxob3N0MB4XDTI2MDUxNzIxMzYxNVoXDTM2MDUx
      NDIxMzYxNVowFDESMBAGA1UEAwwJbG9jYWxob3N0MIIBIjANBgkqhkiG9w0BAQEF
      AAOCAQ8AMIIBCgKCAQEAtDGQu9/koXG7WHo+o+AumoYjkU0MaLLWwlQMu9QhAa9A
      NFEP2HcIZOjyyoXVCp5OcuBo/xjT5xoOC3evGb1MS6Padsb6gdMelhvoXGAWTnNS
      x3DNlgchbWAquiL8bNlUzllv/Fx3SYL0lxJT5Nno2ahvDkBkfUJLwHkS+Zkyu+ai
      VVcuGYEjhkRmaP1yTek0A1LyAdW3Mu8zwahrsVd8Rhbs3vWU1Ws692y/7q4hLIz1
      lO/DA7OMHA8E9ueZ8SiWNntIUc30Ps06o0GZ/W3QfKlVdCe6KUJoT3W6pTYxQEbh
      jyisbJk0RXzYZDMF5Vcjam9ZdnEp6t/d5cIYePjgIQIDAQABo28wbTAdBgNVHQ4E
      FgQU7hiWZDIJfH3Mb4H4nOpAb5kKOQswHwYDVR0jBBgwFoAU7hiWZDIJfH3Mb4H4
      nOpAb5kKOQswDwYDVR0TAQH/BAUwAwEB/zAaBgNVHREEEzARgglsb2NhbGhvc3SH
      BH8AAAEwDQYJKoZIhvcNAQELBQADggEBAB7KV97eUx13opOpBdWlZ15TmpvBOVtp
      2C7l+tafVcjm1fA9dXk5uJ9xLhGP26+604ZGozmIQ8OEhcn2XGEELOTTriPiKVIC
      r2jrjzNELS+JhhgBslzQ27tmotHBiWeNk+no9nPJMXVPA3CBXfT0KccfJUtNd/N9
      xoPycXzSbr81A/fPrrgL2abth0z9Zv7ca1g7/IHFtbSYKWS3V/IHe3WxpUOwcT5I
      bC320mmfbc+d5l7UMoTYSXJ+ksBkfdfObmuMoYcadfJY8m1PkuxhYbSytjREmaS7
      ASbYAzOQzmFIhJeLejkVQAtanXIB44VW6XMjgd6zb2RF2Dc3qzf5mK4=
      -----END CERTIFICATE-----
      """;

  private static final String CLIENT_KEY =
      """
      -----BEGIN PRIVATE KEY-----
      MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCWwr5cWOhOmo8W
      J0ivH3FzOl2dyhalzjKhnn9DMVUBuAefNFVAb2z4XeTniKxu21DaqandXLNDDmA6
      atlzYtdZn4Zz56woDTJT7pwAw0IFC23QjnsSxTRphsSRAj2uOTBFpUFOr2pRbA2N
      ayv7TRS0v5Vi5xzV+iYZcH5t7tMZ4f+M7jBIRYIR92Vy90sEHbZvgGSi2i/8KtwD
      y8LowQ8h1dAXfQf2PM77h+lXNQM58R2yYhryCuC4LqytF62A2Psek9mw8X2V65po
      kwo9w7bcTqOVU0M4++ttP46CZC5Z+m/Q6hDyfqKEehDJEClO3BeDz2VqhAtrS5yz
      mdrx6uWxAgMBAAECggEARODYg+yCxlbLBL4R8SCWfqSt3MdVrY8Vhs2ZKyPAyLZB
      lTUdtgCyco7DZB0HWuaMfDqDxwTxSbOpt0bV7me8J9BAZ3fzKaFN2xbq41ZSNWfR
      VjWieSLsHUJbUD0L3St3qHol6kMueeC6GGBcicXfrezR3YvlmsVc3saMnME+ZVYt
      /2MtZErVXcAO+ZIHj9U8LfyahQLQ9WImItyQmBrxv6AX45ipZF+FZIDAvzpD1a6e
      ATzQONebUZxGEvAkFG+oSv93BVchMpuVk6exEupFRVzAUdqsDmCe3bNn32FIRn/I
      1jmYV0NhB5Tp6M/pJOOIE007qQv/0koIThcLcnjGGQKBgQDLwrybnnhxGPD9gOLw
      pJ/1Obfm2j8KIhjBltCsNvd4qA9VK5Iz3Z4eEU/3SvPVurTe4SCkUJMTty7XbBco
      COa6CmDs4osqZeqeqw3jPTE3JBemuyBuXAzAkkSzXEuelIx/Ob602F7zNuaiYnH/
      nYPxI+UcRKyknGh4vPOTYuIZ7wKBgQC9aYB0gNQC/043++ucnlfPcZdgaNpvKBq5
      wJes7Uxm5j1/l6mIhgziOvZSutnrpXvKZ1mhQ2diOQnj7qXd9GZ97mS8gn4j6LVZ
      hY8ZIRE9kFrqvRG6KVAcum/SYxSu04CECpa9wFGCyMNH1+NQhPb7I7IjnIz1hR7L
      3Vj1Ya8aXwKBgCsitK71I2Y983aXPHPZRguyBl6WwVWx1i/Vb6Oll0oxud079h2I
      wxp2a8lcUrZiajvsyO5AKIK5+u6b8WMGT2H/JHnBk9iL7tlcOPpsT+jueRtQ7hKu
      5fw2etjFpzSRGhUs8lRyodc3PigOUzfb7ryz7qLHXw0SF4PH7IC9JER9AoGAEIpV
      AFd7rCNVVkg2g//D1UarHG0rngdXhh8OX7h9MoMqnajF0jk6iMrzVbghbZWO4Nbz
      mcSEE6y9c10UuPQtuxOFUnzmvQYGSCIEpAIiOkkP123ZTXNsHoYdW7bxSG8Jyv4N
      udZAh/Y2L2Dn5dEmrDmqxbdXC92rGcxjh1IXc2sCgYB3/VbMckDThBPBWi+mD7dJ
      9ncBPciTDVLtNqwR2cKSrTIkyo6og9OLQwozxfi3eNrT791DnyhfMTWo2CyA4mY1
      dvKEWMsaw0BKm0VWAggbBKlmugjPwkbUSLN7sPwdQXONSINDrWSPAPWU/zs5Pse5
      G82SU8Tn0+UQcWmsz9bkBg==
      -----END PRIVATE KEY-----
      """;

  private static final String CLIENT_CERT =
      """
      -----BEGIN CERTIFICATE-----
      MIIDFTCCAf2gAwIBAgIULdVeA2cnfDkBqydj8AUi1ih75hAwDQYJKoZIhvcNAQEL
      BQAwGjEYMBYGA1UEAwwPbWpvLXRlc3QtY2xpZW50MB4XDTI2MDUxNzIxMzYxNloX
      DTM2MDUxNDIxMzYxNlowGjEYMBYGA1UEAwwPbWpvLXRlc3QtY2xpZW50MIIBIjAN
      BgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAlsK+XFjoTpqPFidIrx9xczpdncoW
      pc4yoZ5/QzFVAbgHnzRVQG9s+F3k54isbttQ2qmp3VyzQw5gOmrZc2LXWZ+Gc+es
      KA0yU+6cAMNCBQtt0I57EsU0aYbEkQI9rjkwRaVBTq9qUWwNjWsr+00UtL+VYucc
      1fomGXB+be7TGeH/jO4wSEWCEfdlcvdLBB22b4Bkotov/CrcA8vC6MEPIdXQF30H
      9jzO+4fpVzUDOfEdsmIa8grguC6srRetgNj7HpPZsPF9leuaaJMKPcO23E6jlVND
      OPvrbT+OgmQuWfpv0OoQ8n6ihHoQyRApTtwXg89laoQLa0ucs5na8erlsQIDAQAB
      o1MwUTAdBgNVHQ4EFgQUbJ22RFAjqdw8I5aWr4DNqKqGkn4wHwYDVR0jBBgwFoAU
      bJ22RFAjqdw8I5aWr4DNqKqGkn4wDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0B
      AQsFAAOCAQEAIBgFBEp7E+3tuSmiU+aCMXP9GQCwXIrMr4NsxoBiyQIbzyEkVajT
      pH9Ua9dOyHpiVggCtFGksGnlEue4qFr+9eJY9D7r31WOwbUfskutGj2E5uiD9IND
      Z1jkR3rH6QHYCAVCgDJo+/QHWtEttOGPzNwGf2k5bobl6kD9r7Iv2GXShFJZgBuC
      YDK8zdFFC87cNVrB0hEGBI9IW/O/tIEoCBU5m7oj8JV0KmonUPKEI6QVWaM6bHBf
      yyVuWKrp8TDsjzophv7chM0ewseLhWpSBpprWigM7QMB04CxjbZp1oZmLifT+XKI
      8/e3X7CvXoqWCOPshFn5k+5HDglNHAs16w==
      -----END CERTIFICATE-----
      """;
}
