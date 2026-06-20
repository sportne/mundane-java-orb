/**
 * Local Transaction Service / OTS implementation subset.
 *
 * <p>Durable transaction recovery is an explicit disabled boundary in this package: the supported
 * subset keeps coordinator state in memory and exposes deterministic diagnostics instead of logs,
 * replay, retention, or migration behavior.
 */
package io.github.mundanej.mjo.transaction;
