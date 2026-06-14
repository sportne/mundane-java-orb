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
filter properties, and boolean composition. QoS/admin policy, delivery,
IIOP/Naming, Native Image, interop metadata, and conformance closure remain
staged follow-on work.

## Implemented local surface

- `NotificationServiceOptions` defines bounded local service limits.
- `LocalNotificationService` creates and owns local notification channels.
- `LocalNotificationChannel` exposes supplier and consumer admin handles.
- `LocalNotificationSupplierAdmin` and `LocalNotificationConsumerAdmin` create
  structured proxy handles.
- `LocalNotificationProxy` and its concrete proxy types model local channel
  ownership and lifecycle only; no structured-event delivery is implemented in
  this slice.
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

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
