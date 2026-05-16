# Dependency Locks

Generate locks after the wrapper is bootstrapped:

```bash
./gradlew dependencies --write-locks
```

Commit generated lock files before implementation begins.

Dependency verification metadata: `../verification-metadata.xml`. Refresh it with `./gradlew --write-verification-metadata sha256 help qualityGate` when build dependencies change.
