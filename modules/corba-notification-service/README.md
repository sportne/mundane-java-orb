# corba-notification-service

Notification Service implementation module.

## Current status

Design accepted by ADR-0019. G8-300 splits implementation into narrow promoted
slices. G8-310 adds the local channel lifecycle and Event Service compatibility
boundary: bounded options, stable diagnostics, supplier/consumer admin handles,
structured proxy handles, channel destruction behavior, and explicit proxy-role
mapping back to Event Service roles. G8-320 adds immutable structured events
with fixed identity fields, primitive named properties, bounded variable
header/body sections, duplicate-field rejection, unsupported-value rejection,
and oversized-data diagnostics. G8-330 adds bounded filter parsing and
evaluation for boolean constants, identity equality/inequality, primitive named
filter properties, and boolean composition. G8-340 adds bounded QoS/admin
policy validation for channel, admin, proxy, queue, filter, durable, and
transaction policy keys. G8-350 adds in-JVM structured push/pull delivery with
bounded filters and queues, deterministic queue-limit diagnostics,
failed-consumer removal, stale-proxy diagnostics, and destroyed-channel
behavior. IIOP/Naming, Native Image, interop metadata, and conformance closure
remain staged follow-on work.

## Implemented local surface

- `NotificationServiceOptions` defines bounded local service limits.
- `LocalNotificationService` creates and owns local notification channels.
- `LocalNotificationChannel` exposes supplier and consumer admin handles.
- `LocalNotificationSupplierAdmin` and `LocalNotificationConsumerAdmin` create
  structured proxy handles.
- `LocalNotificationProxy` and its concrete proxy types model local channel
  ownership, lifecycle, local push/pull connections, bounded delivery queues,
  failed-consumer removal, and stale-proxy diagnostics.
- `NotificationProxyKind` maps each local Notification proxy role to its
  compatible Event Service proxy role.
- `NotificationEventCompatibility` records the local compatibility boundary
  between `CosNotification::EventChannel` and
  `CosEventChannelAdmin::EventChannel`.
- `NotificationStructuredEvent`, `NotificationEventIdentity`,
  `NotificationEventType`, `NotificationProperty`, and
  `NotificationPrimitiveValue` define the immutable local structured-event
  value model.
- `NotificationFilter` parses and evaluates the supported local filter subset
  with expression length, depth, and term limits.
- `NotificationPolicies`, `NotificationPolicyKey`, and
  `NotificationPolicyProperty` validate supported local QoS/admin policy
  settings without adding delivery guarantees.
- `NotificationPushConsumer`, `NotificationPushSupplier`,
  `NotificationPullConsumer`, and `NotificationPullSupplier` define the local
  structured callback surface for in-JVM delivery.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
