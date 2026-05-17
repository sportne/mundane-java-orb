# corba-cdr

Common Data Representation reader/writer and CDR golden-wire fixtures.

## Current status

G6 CDR implementation work has started with bounded primitive read/write
behavior.

Implemented behavior:

- explicit big-endian and little-endian primitive encoding;
- centralized CDR alignment and zero padding for writer output;
- bounded writer output using shared `BoundedLimit` values;
- strict boolean decoding for octets `0` and `1`;
- primitive boolean, octet, char, short, unsigned short, long, unsigned long,
  long long, unsigned long long, float, double, and raw 16-octet long double
  payloads.

Native Image validation:

- `./gradlew :modules:corba-cdr:nativeCdrSmoke` builds and runs a GraalVM
  Native Image smoke executable for the CDR primitive API when `native-image`
  is available.

Strings, sequences, arrays, encapsulations, TypeCode, Any, GIOP/IIOP transport,
IOR profiles, ORB/runtime behavior, and generated codecs remain future roadmap
work.

## Documentation requirements

- Add package documentation for every public package.
- Update relevant architecture and conformance docs before implementing behavior.
- Link implementation tasks to requirement IDs.
