# CDR, GIOP, and IIOP Design

## CDR rules

- All reads validate remaining bytes.
- All length fields validate configured maximums before allocation.
- Alignment logic is centralized.
- Endian behavior is explicit.
- Encapsulation handling is separate from top-level message handling.
- Generated codecs call CDR APIs directly.

## GIOP rules

- Every message kind requires golden-wire tests.
- Fragmentation must be bounded.
- Service contexts must be parsed with configurable limits.
- Unknown or unsupported message behavior must be profile-defined.

## IIOP rules

- TCP transport must support timeouts and backpressure.
- TLS/mTLS support must be explicit and testable.
- Connection pooling and request correlation must be observable.
