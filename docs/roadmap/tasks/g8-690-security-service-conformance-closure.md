# G8-690 Security Service Conformance Closure

Task ID: G8-690-SECURITY-SERVICE-CONFORMANCE-CLOSURE
Status: blocked
Gate: Optional CORBA service implementation
Requirement IDs: REQ-SVC-050, REQ-IIOP-002, REQ-NATIVE-002, REQ-INTEROP-009, REQ-SEC-006, REQ-DOC-006
ADR IDs: ADR-0001, ADR-0002, ADR-0004, ADR-0005, ADR-0010, ADR-0016, ADR-0022
Specification references: SEC-18, CORBA-IOP-SECURITY
Target module: modules/corba-security-service
Allowed files: modules/corba-security-service/README.md, modules/corba-services-core/README.md, docs/architecture/services-design.md, docs/architecture/cdr-giop-iiop.md, docs/conformance/optional-services-matrix.md, docs/requirements/service-requirements.md, docs/verification/optional-services-review.md, docs/verification/interop-matrix.md, docs/verification/native-image-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g8-690-security-service-conformance-closure.md, README.md
Forbidden files: runtime implementation changes, public API implementation changes, live peer execution, committed live interop reports, peer artifacts, Docker layers, prepared peer images, generated OMG APIs, broad secure peer compatibility claims, automatic TLS policy changes, global JVM security-manager integration, enterprise identity management, Native Image binary commits, Java serialization metadata, reflection metadata, dynamic proxies, runtime bytecode generation, `Unsafe`, `sun.*`, `jdk.internal.*`
Expected behavior: Task type: implementation. Close the Security Service local conformance record across credential/trust behavior, policy validation, CSIv2 metadata, local policy evaluation, audit/failure disclosure, loopback IIOP boundary, Native Image smoke, and interop dry-run metadata. Do not add runtime behavior or claim live secure peer results, enterprise identity management, automatic TLS policy, or global JVM security integration.
Tests to add/update: No new runtime tests unless documentation verification reveals a missing assertion from earlier slices. Confirm existing Security Service, Native Image, and interop metadata tests cover the closed record.
Documentation to update: Security Service README, services design, CDR/GIOP/IIOP architecture, optional services conformance/review, interop matrix, Native Image matrix, roadmap index, README, and next ready roadmap task if required by design-control gates.
Commands to run: ./gradlew :modules:corba-security-service:test :modules:corba-services-core:test :modules:corba-iiop:test :modules:corba-native-image:test :modules:corba-interop-testkit:test; ./interop/bin/interop-peer validate-manifests; ./interop/bin/interop-peer run-direction-matrix --dry-run security-service all; ./gradlew test; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Security Service conformance documentation matches implemented local/IIOP/Native Image/dry-run behavior; live secure peer claims, enterprise identity management, automatic TLS policy, and global JVM security integration remain unapproved; the next existing roadmap task is promoted only if design-control rules require a ready successor.
Rollback notes: Revert Security Service conformance documentation and roadmap status updates together.
