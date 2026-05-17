# CDR, GIOP, and IIOP Design

## CDR rules

- All reads validate remaining bytes.
- All length fields validate configured maximums before allocation.
- Alignment logic is centralized.
- Endian behavior is explicit.
- Encapsulation handling is separate from top-level message handling.
- Generated codecs call CDR APIs directly.

## CDR Primitive Slice

`modules/corba-cdr` exposes syntax-independent primitive readers and writers.
The first implementation slice covers explicit byte order, primitive alignment,
bounded writer output, and deterministic primitive wire encodings only.

Primitive coverage includes boolean, octet, char, short, unsigned short, long,
unsigned long, long long, unsigned long long, float, double, and raw 16-octet
long double payloads. Boolean decoding is strict: only octets `0` and `1` are
accepted. Long double numeric interpretation is deferred; this slice preserves
the 16-octet payload.

String, sequence, array, encapsulation, TypeCode, Any, object-reference, GIOP,
IIOP, and ORB invocation behavior remain outside the primitive slice.

The primitive reader/writer package participates in the Native Image validation
lane. Its native smoke executable exercises deterministic CDR primitive wire
behavior without reflection, dynamic proxies, runtime code generation, or
native-image metadata.

## GIOP rules

- Every message kind requires golden-wire tests.
- Fragmentation must be bounded.
- Service contexts must be parsed with configurable limits.
- Unknown or unsupported message behavior must be profile-defined.

## IIOP rules

- TCP transport must support timeouts and backpressure.
- TLS/mTLS support must be explicit and testable.
- Connection pooling and request correlation must be observable.
