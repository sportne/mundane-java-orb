# G10-100 Native Image Interop Binaries

Task ID: G10-100-NATIVE-IMAGE-INTEROP-BINARIES
Status: complete
Gate: G10 pre-1.0 interoperability
Requirement IDs: REQ-NATIVE-001, REQ-NATIVE-002, REQ-NATIVE-003, REQ-NATIVE-004, REQ-INTEROP-002, REQ-INTEROP-004, REQ-INTEROP-009
ADR IDs: ADR-0008, ADR-0010
Specification references: CORBA-IOP-CDR, CORBA-IOP-GIOP, CORBA-IOP-IIOP, NAM-13, JAV2I-14-RMI-IDL
Target module: modules/corba-native-image and interop
Allowed files: modules/corba-native-image/src/**, modules/corba-native-image/build.gradle, interop/**, docs/architecture/native-image-design.md, docs/build/toolchain-matrix.md, docs/verification/native-image-matrix.md, docs/verification/interop-matrix.md, docs/roadmap/roadmap-index.md, docs/roadmap/tasks/g10-100-native-image-interop-binaries.md, docs/roadmap/tasks/g10-110-real-peer-harness-closure.md, README.md
Forbidden files: peer artifacts, committed native binaries, live interop reports, optional service implementation, reflection metadata unless explicitly reviewed
Expected behavior: Task type: implementation. Built native client/server smoke binaries for the implemented interop scenarios using the available GraalVM Native Image toolchain, and classified native-lane missing prerequisites deterministically in reports.
Tests to add/update: JVM parity tests, source-level metadata audits, Native Image smoke builds where available, interop report classification tests, and no-metadata regression tests.
Documentation to update: Native Image design, toolchain matrix, Native Image matrix, interop matrix, roadmap index, README ready-task status, this task, and G10-110 status when complete.
Commands to run: ./gradlew :modules:corba-native-image:test :modules:corba-native-image:nativeImageBinariesSmoke; ./interop/bin/interop-peer validate-manifests; ./interop/bin/interop-peer validate-gates; ./gradlew validateDesignControlPack qualityGate; git diff --check
Acceptance criteria: Native client/server binaries exist for the completed G10 local interop lanes when Native Image is available; unavailable toolchains or missing binary paths produce structured infrastructure reports rather than silent omissions.
Rollback notes: Revert native-image build, smoke, interop harness, test, and documentation changes together.
