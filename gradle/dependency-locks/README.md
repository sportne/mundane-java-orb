# Dependency Locks

Generate locks after the wrapper is bootstrapped:

```bash
./gradlew dependencies --write-locks
```

Commit generated lock files before implementation begins.

Dependency verification template: `../verification-metadata.template.xml`. Generate the real metadata file in a connected environment.
