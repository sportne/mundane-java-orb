# corba-event-service

Event Service implementation module.

## Current status

Design accepted by ADR-0018. G8-210 adds the local channel lifecycle model:
bounded options, stable diagnostics, supplier/consumer admin surfaces, and
proxy handles. G8-220 adds local push and pull delivery. G8-230 adds local
backpressure and stale/failed proxy diagnostics while keeping all behavior
in-JVM. IIOP/Naming exposure, Native Image smoke, interop metadata, and live
peer claims remain staged follow-on work.

## Implemented local surface

- `EventServiceOptions` defines bounded local service limits.
- `LocalEventService` creates and owns local event channels.
- `LocalEventChannel` exposes supplier and consumer admin handles.
- `LocalEventSupplierAdmin` and `LocalEventConsumerAdmin` create proxy handles.
- `LocalEventProxy` and its concrete proxy types model local channel ownership
  and callback connection lifecycle.
- `EventPushConsumer`, `EventPushSupplier`, `EventPullConsumer`, and
  `EventPullSupplier` define local push and pull callback shapes.
- `LocalPushConsumerProxy.push` routes in-JVM push events to connected local
  push consumers.
- `LocalPullSupplierProxy.pull` and `tryPull` route in-JVM pull requests to
  connected local pull suppliers.
- Configured channel, proxy, and pending fan-out limits fail with stable
  diagnostics.
- Failed push consumers are removed from active local routing and later stale
  proxy operations fail deterministically.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
