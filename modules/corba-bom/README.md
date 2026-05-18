# corba-bom

Java platform BOM for aligning all mundane Java ORB artifacts.

## Current status

G6 release validation checks this BOM against the set of published modules. The
BOM is also used by the standalone offline release consumer to prove downstream
builds can resolve aligned artifacts from a Maven repository.

## Validation

- `validateBomAlignment` rejects missing or stale project constraints.
- `validatePublicationDryRun` checks the staged BOM POM and Gradle module
  metadata.
- `validateDownstreamSampleConsumer` imports the BOM from the staged repository
  without project substitution.
