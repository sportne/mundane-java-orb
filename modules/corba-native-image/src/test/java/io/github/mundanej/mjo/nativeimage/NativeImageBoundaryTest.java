package io.github.mundanej.mjo.nativeimage;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;

final class NativeImageBoundaryTest {

  @Test
  void nativeImageSourcesDoNotIntroduceDynamicMetadataOrHostileMechanisms() throws IOException {
    Path module = Path.of("").toAbsolutePath();
    Path modules = Objects.requireNonNull(module.getParent(), "module parent");
    Path repository = Objects.requireNonNull(modules.getParent(), "repository root");
    List<Path> sourceRoots =
        List.of(
            module.resolve("src/main"),
            module.resolve("src/nativeSmoke"),
            repository.resolve("modules/corba-rmi-iiop/src/main"),
            repository.resolve("modules/corba-notification-service/src/main"),
            repository.resolve("modules/corba-trading-service/src/main"),
            repository.resolve("modules/corba-transaction-service/src/main"),
            repository.resolve("modules/corba-security-service/src/main"));
    List<String> forbiddenTokens =
        List.of(
            "reflect-config.json",
            "proxy-config.json",
            "serialization-config.json",
            "java.lang.reflect",
            "Proxy.newProxyInstance",
            "ServiceLoader",
            "ClassLoader",
            "ProcessBuilder",
            "Runtime.getRuntime",
            "javax.script",
            "ScriptEngine",
            "ObjectInputStream",
            "ObjectOutputStream",
            "java.io.Serializable",
            "net.bytebuddy",
            "org.objectweb.asm",
            "org.cglib",
            "jdk.internal",
            "sun.",
            "Unsafe");

    for (Path root : sourceRoots) {
      try (var paths = Files.walk(root)) {
        for (Path source : paths.filter(Files::isRegularFile).toList()) {
          String content = Files.readString(source);
          for (String token : forbiddenTokens) {
            assertFalse(content.contains(token), source + " must not contain " + token);
          }
        }
      }
    }
  }
}
