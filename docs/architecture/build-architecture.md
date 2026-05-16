# Build Architecture

## Build system

- Gradle 9.x, pinned wrapper version.
- Groovy DSL.
- Version catalog.
- Convention plugins in `build-logic`.
- Dependency locking and verification after G4 completion.
- Offline build support through `-PcorbaOfflineRepo=/path/to/repo`.

## Quality tools

- JUnit Platform/Jupiter.
- ArchUnit.
- JaCoCo.
- Spotless.
- Checkstyle.
- SpotBugs.
- Error Prone.
- GraalVM Native Build Tools.

## Build matrix

- OpenJDK 21 JVM.
- OpenJDK 25 JVM.
- GraalVM JDK 21 JVM.
- GraalVM JDK 25 JVM.
- GraalVM JDK 21 Native Image.
- GraalVM JDK 25 Native Image.
