package io.github.mundanej.mjo.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class NotificationPoliciesTest {

  @Test
  void providesBoundedDefaults() {
    NotificationPolicies policies = NotificationPolicies.defaults();

    assertEquals(NotificationPolicies.DEFAULT_QUEUE_LIMIT, policies.queueLimit());
    assertEquals(NotificationServiceOptions.DEFAULT_MAX_CHANNELS, policies.channelLimit());
    assertEquals(
        NotificationServiceOptions.DEFAULT_MAX_SUPPLIERS_PER_CHANNEL,
        policies.supplierAdminLimit());
    assertEquals(
        NotificationServiceOptions.DEFAULT_MAX_CONSUMERS_PER_CHANNEL,
        policies.consumerAdminLimit());
    assertEquals(
        NotificationServiceOptions.DEFAULT_MAX_SUPPLIERS_PER_CHANNEL
            + NotificationServiceOptions.DEFAULT_MAX_CONSUMERS_PER_CHANNEL,
        policies.proxyLimit());
    assertEquals(NotificationFilter.MAX_EXPRESSION_LENGTH, policies.filterLengthLimit());
    assertEquals(NotificationFilter.MAX_DEPTH, policies.filterDepthLimit());
    assertEquals(NotificationFilter.MAX_TERMS, policies.filterTermLimit());
    assertFalse(policies.durable());
    assertFalse(policies.transacted());
  }

  @Test
  void acceptsCallerConfiguredLimits() {
    NotificationPolicies policies =
        NotificationPolicies.from(
            List.of(
                NotificationPolicyProperty.signedLong("queue-limit", 8),
                NotificationPolicyProperty.signedLong("channel-limit", 2),
                NotificationPolicyProperty.signedLong("supplier-admin-limit", 3),
                NotificationPolicyProperty.signedLong("consumer-admin-limit", 4),
                NotificationPolicyProperty.signedLong("proxy-limit", 7),
                NotificationPolicyProperty.signedLong("filter-length-limit", 128),
                NotificationPolicyProperty.signedLong("filter-depth-limit", 4),
                NotificationPolicyProperty.signedLong("filter-term-limit", 8),
                NotificationPolicyProperty.bool("durable", false),
                NotificationPolicyProperty.bool("transacted", false)));

    assertEquals(8, policies.queueLimit());
    assertEquals(2, policies.channelLimit());
    assertEquals(3, policies.supplierAdminLimit());
    assertEquals(4, policies.consumerAdminLimit());
    assertEquals(7, policies.proxyLimit());
    assertEquals(128, policies.filterLengthLimit());
    assertEquals(4, policies.filterDepthLimit());
    assertEquals(8, policies.filterTermLimit());
  }

  @Test
  void rejectsUnsupportedPolicyKeys() {
    NotificationServiceException exception =
        assertThrows(
            NotificationServiceException.class,
            () -> NotificationPolicyProperty.signedLong("priority-order", 1));

    assertEquals(NotificationServiceDiagnosticCodes.UNSUPPORTED_POLICY, exception.code());
  }

  @Test
  void rejectsDuplicatePolicyKeys() {
    NotificationServiceException exception =
        assertThrows(
            NotificationServiceException.class,
            () ->
                NotificationPolicies.from(
                    List.of(
                        NotificationPolicyProperty.signedLong("queue-limit", 8),
                        NotificationPolicyProperty.signedLong("queue-limit", 9))));

    assertEquals(NotificationServiceDiagnosticCodes.DUPLICATE_POLICY, exception.code());
  }

  @Test
  void rejectsMalformedPolicyTypes() {
    NotificationServiceException integer =
        assertThrows(
            NotificationServiceException.class,
            () ->
                NotificationPolicies.from(
                    List.of(NotificationPolicyProperty.bool("queue-limit", true))));
    NotificationServiceException bool =
        assertThrows(
            NotificationServiceException.class,
            () ->
                NotificationPolicies.from(
                    List.of(NotificationPolicyProperty.signedLong("durable", 1))));

    assertEquals(NotificationServiceDiagnosticCodes.MALFORMED_POLICY, integer.code());
    assertEquals(NotificationServiceDiagnosticCodes.MALFORMED_POLICY, bool.code());
  }

  @Test
  void rejectsMalformedNullPolicyInputs() {
    List<NotificationPolicyProperty> properties = new ArrayList<>();
    properties.add(null);

    NotificationServiceException nullList =
        assertThrows(NotificationServiceException.class, () -> NotificationPolicies.from(null));
    NotificationServiceException nullEntry =
        assertThrows(
            NotificationServiceException.class, () -> NotificationPolicies.from(properties));
    NotificationServiceException nullKey =
        assertThrows(
            NotificationServiceException.class,
            () ->
                new NotificationPolicyProperty(
                    null, NotificationPrimitiveValue.booleanValue(true)));
    NotificationServiceException nullValue =
        assertThrows(
            NotificationServiceException.class,
            () -> new NotificationPolicyProperty(NotificationPolicyKey.DURABLE, null));

    assertEquals(NotificationServiceDiagnosticCodes.MALFORMED_POLICY, nullList.code());
    assertEquals(NotificationServiceDiagnosticCodes.MALFORMED_POLICY, nullEntry.code());
    assertEquals(NotificationServiceDiagnosticCodes.MALFORMED_POLICY, nullKey.code());
    assertEquals(NotificationServiceDiagnosticCodes.MALFORMED_POLICY, nullValue.code());
  }

  @Test
  void rejectsOutOfRangePolicyValues() {
    NotificationServiceException low =
        assertThrows(
            NotificationServiceException.class,
            () ->
                NotificationPolicies.from(
                    List.of(NotificationPolicyProperty.signedLong("queue-limit", 0))));
    NotificationServiceException high =
        assertThrows(
            NotificationServiceException.class,
            () ->
                NotificationPolicies.from(
                    List.of(
                        NotificationPolicyProperty.signedLong(
                            "filter-depth-limit", NotificationFilter.MAX_DEPTH + 1L))));

    assertEquals(NotificationServiceDiagnosticCodes.POLICY_LIMIT_EXCEEDED, low.code());
    assertEquals(NotificationServiceDiagnosticCodes.POLICY_LIMIT_EXCEEDED, high.code());
  }

  @Test
  void rejectsUnsupportedDurableAndTransactionRequests() {
    NotificationServiceException durable =
        assertThrows(
            NotificationServiceException.class,
            () ->
                NotificationPolicies.from(
                    List.of(NotificationPolicyProperty.bool("durable", true))));
    NotificationServiceException transacted =
        assertThrows(
            NotificationServiceException.class,
            () ->
                NotificationPolicies.from(
                    List.of(NotificationPolicyProperty.bool("transacted", true))));

    assertEquals(NotificationServiceDiagnosticCodes.UNSUPPORTED_POLICY, durable.code());
    assertEquals(NotificationServiceDiagnosticCodes.UNSUPPORTED_POLICY, transacted.code());
  }

  @Test
  void rejectsConflictingProxyPolicyLimits() {
    NotificationServiceException exception =
        assertThrows(
            NotificationServiceException.class,
            () ->
                NotificationPolicies.from(
                    List.of(
                        NotificationPolicyProperty.signedLong("supplier-admin-limit", 8),
                        NotificationPolicyProperty.signedLong("proxy-limit", 7))));

    assertEquals(NotificationServiceDiagnosticCodes.CONFLICTING_POLICY, exception.code());
  }
}
