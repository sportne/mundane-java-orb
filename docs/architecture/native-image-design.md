# Native Image Design

## Rules

- Generate stubs, skeletons, codecs, descriptors, and metadata at build time.
- Avoid reflection in normal paths.
- Avoid dynamic proxies in core runtime.
- Avoid runtime bytecode generation.
- Avoid Java serialization for normal CORBA marshaling.
- Document runtime class initialization choices.

## Native test targets

- `idlj` CLI.
- client sample.
- server sample.
- naming server.
- selected interop clients and servers.
