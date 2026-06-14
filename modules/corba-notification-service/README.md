# corba-notification-service

Notification Service implementation module.

## Current status

Design accepted by ADR-0019. G8-300 splits implementation into narrow promoted
slices. The next ready slice is
`G8-310-NOTIFICATION-SERVICE-EVENT-COMPATIBILITY-BOUNDARY`, which will add the
local channel lifecycle and Event Service compatibility boundary before
structured events, filters, QoS/admin policy, delivery, IIOP/Naming, Native
Image, interop metadata, or conformance closure.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
