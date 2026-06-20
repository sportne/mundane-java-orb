package io.github.mundanej.mjo.security;

import java.util.List;
import java.util.Objects;

/** Loopback IIOP boundary for local CSIv2 metadata and policy evaluation. */
public final class SecurityIiopBoundary implements AutoCloseable {

  private final SecurityPolicyEvaluator evaluator;
  private final SecurityIiopContextCodec codec;
  private final SecurityAuditDisclosureModel auditModel;
  private boolean closed;

  /** Creates a boundary backed by default local evaluator, codec, and audit model. */
  public SecurityIiopBoundary() {
    this(
        new SecurityPolicyEvaluator(),
        new SecurityIiopContextCodec(),
        new SecurityAuditDisclosureModel());
  }

  /** Creates a boundary backed by caller-provided evaluator and default codecs. */
  public SecurityIiopBoundary(SecurityPolicyEvaluator evaluator) {
    this(evaluator, new SecurityIiopContextCodec(), new SecurityAuditDisclosureModel());
  }

  /** Creates a boundary backed by caller-provided local collaborators. */
  public SecurityIiopBoundary(
      SecurityPolicyEvaluator evaluator,
      SecurityIiopContextCodec codec,
      SecurityAuditDisclosureModel auditModel) {
    this.evaluator = Objects.requireNonNull(evaluator, "evaluator");
    this.codec = Objects.requireNonNull(codec, "codec");
    this.auditModel = Objects.requireNonNull(auditModel, "auditModel");
  }

  /** Exports CSIv2 metadata as a local IIOP service context descriptor. */
  public synchronized SecurityIiopContextDescriptor exportServiceContext(
      SecurityCsiv2MetadataSnapshot metadata) {
    requireOpen();
    return codec.encodeServiceContext(metadata);
  }

  /** Exports CSIv2 metadata as a local IOR tagged component descriptor. */
  public synchronized SecurityIiopContextDescriptor exportTaggedComponent(
      SecurityCsiv2MetadataSnapshot metadata) {
    requireOpen();
    return codec.encodeTaggedComponent(metadata);
  }

  /** Evaluates local policy using one CSIv2 service context when present. */
  public synchronized SecurityPolicyEvaluationDecision evaluateServiceContexts(
      SecurityPolicySnapshot policy,
      List<SecurityIiopContextDescriptor> descriptors,
      SecurityTrustEvaluationInput credential) {
    requireOpen();
    SecurityCsiv2MetadataSnapshot metadata =
        codec.findServiceContext(descriptors).map(codec::decode).orElse(null);
    return evaluator.evaluate(new SecurityPolicyEvaluationRequest(policy, metadata, credential));
  }

  /** Evaluates local policy using one CSIv2 tagged component when present. */
  public synchronized SecurityPolicyEvaluationDecision evaluateTaggedComponents(
      SecurityPolicySnapshot policy,
      List<SecurityIiopContextDescriptor> descriptors,
      SecurityTrustEvaluationInput credential) {
    requireOpen();
    SecurityCsiv2MetadataSnapshot metadata =
        codec.findTaggedComponent(descriptors).map(codec::decode).orElse(null);
    return evaluator.evaluate(new SecurityPolicyEvaluationRequest(policy, metadata, credential));
  }

  /** Evaluates local policy and creates a redacted audit event for the IIOP decision. */
  public synchronized SecurityAuditEvent auditServiceContexts(
      SecurityPolicySnapshot policy,
      List<SecurityIiopContextDescriptor> descriptors,
      SecurityTrustEvaluationInput credential,
      List<SecurityAuditField> fields) {
    requireOpen();
    return auditModel.event(evaluateServiceContexts(policy, descriptors, credential), fields);
  }

  /** Returns whether this local IIOP boundary is closed. */
  public synchronized boolean isClosed() {
    return closed;
  }

  /** Closes the local boundary without starting or stopping peer execution. */
  @Override
  public synchronized void close() {
    closed = true;
  }

  private void requireOpen() {
    if (closed) {
      throw new SecurityServiceException(
          SecurityServiceDiagnosticCodes.IIOP_SECURITY_BOUNDARY_CLOSED,
          "Security Service IIOP boundary is closed");
    }
  }
}
