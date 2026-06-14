/**
 * Local Notification Service channel lifecycle and Event Service compatibility boundary.
 *
 * <p>This package intentionally starts with local, explicit runtime objects and an immutable
 * structured-event value model, bounded filter evaluator, and QoS/admin policy validation.
 * Delivery, IIOP exposure, Native Image smoke, and peer interop are staged by later roadmap tasks.
 */
package io.github.mundanej.mjo.notification;
