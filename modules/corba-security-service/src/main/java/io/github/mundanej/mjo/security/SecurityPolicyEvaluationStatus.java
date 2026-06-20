package io.github.mundanej.mjo.security;

/** Local Security Service policy evaluation outcomes. */
public enum SecurityPolicyEvaluationStatus {
  /** The local policy subset accepts the supplied inputs. */
  ALLOW,

  /** The local policy subset requires credentials before it can allow the request. */
  CHALLENGE,

  /** The local policy subset rejects the supplied inputs. */
  DENY
}
