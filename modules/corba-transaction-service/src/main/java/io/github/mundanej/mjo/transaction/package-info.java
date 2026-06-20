/**
 * Local Transaction Service / OTS implementation subset.
 *
 * <p>Durable transaction recovery is an explicit disabled boundary in this package: the supported
 * subset keeps coordinator state in memory and exposes deterministic diagnostics instead of logs,
 * replay, retention, or migration behavior.
 *
 * <p>IIOP request-context support is descriptor-backed and carries only the local propagation
 * metadata subset. It does not add peer distributed two-phase commit or durable recovery behavior.
 */
package io.github.mundanej.mjo.transaction;
