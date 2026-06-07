# corba-event-service

Event Service implementation module.

## Current status

Design accepted by ADR-0018. G8-210 adds the local channel lifecycle model:
bounded options, stable diagnostics, supplier/consumer admin surfaces, and
proxy handles. Local push/pull delivery, backpressure, IIOP/Naming exposure,
Native Image smoke, interop metadata, and live peer claims remain staged
follow-on work.

## Implemented local surface

- `EventServiceOptions` defines bounded local service limits.
- `LocalEventService` creates and owns local event channels.
- `LocalEventChannel` exposes supplier and consumer admin handles.
- `LocalEventSupplierAdmin` and `LocalEventConsumerAdmin` create proxy handles.
- `LocalEventProxy` and its concrete proxy types model local channel ownership.
- `EventPushConsumer`, `EventPushSupplier`, `EventPullConsumer`, and
  `EventPullSupplier` define callback shapes for later local delivery slices.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
