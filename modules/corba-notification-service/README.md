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
behavior. G8-360 adds descriptor-backed loopback IIOP/Naming exposure for
channel/admin lookup, structured proxy creation, structured push/pull
operations, local filter/QoS rejection diagnostics, malformed request
diagnostics, Naming-resolved NotificationChannel IORs, and clean shutdown.
G8-370 adds representative Native Image smoke coverage for channel creation,
structured-event validation, filter validation, QoS rejection, local delivery,
loopback IIOP/Naming exposure, and clean shutdown. G8-380 adds metadata-only
interop discovery for the `notification-service` scenario: an IDL fixture,
approved-peer manifest declarations, deterministic JVM/native dry-run direction
enumeration, and structured missing-prerequisite reports. G8-390 closes the
local/IIOP/Native Image/dry-run conformance record for the implemented subset.
Live peer execution and pass/fail peer compatibility claims remain out of
scope.

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
  settings used by local delivery and loopback diagnostics.
- `NotificationPushConsumer`, `NotificationPushSupplier`,
  `NotificationPullConsumer`, and `NotificationPullSupplier` define the local
  structured callback surface for in-JVM delivery.
- `NotificationServiceDescriptors`, `NotificationServiceIiopCodec`,
  `NetworkNotificationService`, and `NetworkNotificationServiceClient` expose
  the supported subset over loopback IIOP and optional Naming.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
