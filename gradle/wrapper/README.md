# Gradle Wrapper

This scaffold pins Gradle to 9.5.1 through `gradle-wrapper.properties`.

The standard Gradle wrapper scripts and `gradle-wrapper.jar` are committed so contributors can run the build without a system Gradle installation.

For offline environments, mirror the Gradle 9.5.1 distribution and configure the
wrapper distribution URL according to the organization's internal artifact policy.
