# CDR, GIOP, and IIOP Design

## CDR rules

- All reads validate remaining bytes.
- All length fields validate configured maximums before allocation.
- Alignment logic is centralized.
- Endian behavior is explicit.
- Encapsulation handling is separate from top-level message handling.
- Generated codecs call CDR APIs directly.

## CDR Reader/Writer Slices

`modules/corba-cdr` exposes syntax-independent primitive readers and writers.
The first implementation slices cover explicit byte order, primitive alignment,
bounded writer output, deterministic primitive wire encodings, and bounded
length-bearing values.

Primitive coverage includes boolean, octet, char, short, unsigned short, long,
unsigned long, long long, unsigned long long, float, double, and raw 16-octet
long double payloads. Boolean decoding is strict: only octets `0` and `1` are
accepted. Long double numeric interpretation is deferred; this slice preserves
the 16-octet payload.

Length-bearing CDR coverage includes narrow strings, sequence lengths,
fixed-array element-count validation for generated-code loops, raw octet
sequences, and length-prefixed encapsulations. Narrow strings use one-octet
Latin-1 mapping in this CDR slice because code-set negotiation is GIOP-level
behavior. Encapsulations validate byte-order markers and create nested readers
whose alignment starts at the encapsulation stream, after the marker has been
consumed.

Wstring, negotiated code sets, TypeCode, Any, object-reference, GIOP, IIOP, and
ORB invocation behavior remain outside the current CDR slice.

The primitive reader/writer package participates in the Native Image validation
lane. Its native smoke executable exercises deterministic CDR primitive,
string, sequence, and encapsulation behavior without reflection, dynamic
proxies, runtime code generation, or native-image metadata.

## GIOP rules

- Every message kind requires golden-wire tests.
- Fragmentation must be bounded.
- Service contexts must be parsed with configurable limits.
- Unknown or unsupported message behavior must be profile-defined.

## IIOP rules

- TCP transport must support timeouts and backpressure.
- TLS/mTLS support must be explicit and testable.
- Connection pooling and request correlation must be observable.
