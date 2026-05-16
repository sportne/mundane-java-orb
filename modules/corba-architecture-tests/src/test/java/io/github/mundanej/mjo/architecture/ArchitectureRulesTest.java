package io.github.mundanej.mjo.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Repository-wide architectural boundary checks. */
@Tag("architecture")
final class ArchitectureRulesTest {

  private static final Pattern OMG_PACKAGE_DECLARATION =
      Pattern.compile("^\\s*package\\s+org\\.omg(\\.|;).*");
  private static final Path REPOSITORY_ROOT = findRepositoryRoot();
  private static final Path MODULES_DIRECTORY = REPOSITORY_ROOT.resolve("modules");
  private static final String[] IDL_MODULE_PACKAGES = {"..codegen..", "..idl..", "..idlj.."};
  private static final String[] NORMAL_RUNTIME_PACKAGES = {
    "..any..",
    "..cdr..",
    "..dynamic..",
    "..giop..",
    "..iiop..",
    "..ior..",
    "..naming..",
    "..orb..",
    "..poa..",
    "..repositoryid..",
    "..service..",
    "..services..",
    "..typecode.."
  };
  private static final String[] PROTOCOL_PACKAGES = {"..cdr..", "..giop..", "..iiop..", "..ior.."};
  private static final String[] SERVICE_PACKAGES = {
    "..event..",
    "..naming..",
    "..notification..",
    "..security..",
    "..service..",
    "..services..",
    "..time..",
    "..trading..",
    "..transaction.."
  };
  private static final String[] TRANSPORT_AND_PROTOCOL_PACKAGES = {
    "..cdr..", "..giop..", "..iiop..", "..ior.."
  };
  private static final String[] UPPER_PROTOCOL_RUNTIME_AND_SERVICE_PACKAGES = {
    "..event..",
    "..giop..",
    "..iiop..",
    "..naming..",
    "..notification..",
    "..orb..",
    "..poa..",
    "..security..",
    "..service..",
    "..services..",
    "..time..",
    "..trading..",
    "..transaction.."
  };

  @BeforeAll
  static void allowEmptyRulesDuringScaffoldPhase() {
    ArchConfiguration.get().setProperty("archRule.failOnEmptyShould", "false");
  }

  @Test
  void onlyOmgApiMayDefineLegacyOmgPackages() throws IOException {
    Path allowedModule = MODULES_DIRECTORY.resolve("corba-omg-api").normalize();
    List<Path> violations;

    try (var paths =
        Files.find(MODULES_DIRECTORY, Integer.MAX_VALUE, ArchitectureRulesTest::isJavaSourceFile)) {
      violations =
          paths
              .filter(path -> !path.normalize().startsWith(allowedModule))
              .filter(ArchitectureRulesTest::declaresLegacyOmgPackage)
              .toList();
    }

    assertTrue(
        violations.isEmpty(),
        () -> "Only modules/corba-omg-api may define org.omg.* packages: " + violations);
  }

  @Test
  void idlModulesMustNotDependOnTransportOrProtocolPackages() {
    JavaClasses classes = projectClasses();

    noClasses()
        .that()
        .resideInAnyPackage(IDL_MODULE_PACKAGES)
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(TRANSPORT_AND_PROTOCOL_PACKAGES)
        .check(classes);
  }

  @Test
  void cdrMustNotDependOnUpperProtocolRuntimeOrServicePackages() {
    JavaClasses classes = projectClasses();

    noClasses()
        .that()
        .resideInAnyPackage("..cdr..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(UPPER_PROTOCOL_RUNTIME_AND_SERVICE_PACKAGES)
        .check(classes);
  }

  @Test
  void giopMustNotDependOnOrbCorePoaOrServices() {
    JavaClasses classes = projectClasses();

    noClasses()
        .that()
        .resideInAnyPackage("..giop..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(merge("..orb..", "..poa..", SERVICE_PACKAGES))
        .check(classes);
  }

  @Test
  void iiopMustNotDependOnOrbCorePoaServicesOrIdlPackages() {
    JavaClasses classes = projectClasses();

    noClasses()
        .that()
        .resideInAnyPackage("..iiop..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(merge("..orb..", "..poa..", SERVICE_PACKAGES, IDL_MODULE_PACKAGES))
        .check(classes);
  }

  @Test
  void protocolModulesMustNotDependOnOrbCoreOrPoa() {
    JavaClasses classes = projectClasses();

    noClasses()
        .that()
        .resideInAnyPackage(PROTOCOL_PACKAGES)
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..orb..", "..poa..")
        .check(classes);
  }

  @Test
  void coreProtocolModulesMustNotUseReflection() {
    JavaClasses classes = projectClasses();

    noClasses()
        .that()
        .resideInAnyPackage(PROTOCOL_PACKAGES)
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("java.lang.reflect..")
        .check(classes);
  }

  @Test
  void normalMarshallingModulesMustNotUseJavaSerialization() {
    JavaClasses classes = projectClasses();

    noClasses()
        .that()
        .resideInAnyPackage("..cdr..", "..codegen..", "..giop..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("java.io..")
        .check(classes);
  }

  @Test
  void normalRuntimePathsMustNotUseBytecodeGenerationOrInternalJdkApis() {
    JavaClasses classes = projectClasses();

    noClasses()
        .that()
        .resideInAnyPackage(NORMAL_RUNTIME_PACKAGES)
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "java.lang.instrument..",
            "javax.tools..",
            "jdk.internal..",
            "net.bytebuddy..",
            "org.cglib..",
            "org.objectweb.asm..",
            "sun..")
        .check(classes);
  }

  @Test
  void commonFoundationPackageMustExistOnceImplementationStarts() throws IOException {
    Path commonSources =
        MODULES_DIRECTORY.resolve("corba-common/src/main/java/io/github/mundanej/mjo/common");

    assertTrue(
        Files.isDirectory(commonSources) && containsJavaSource(commonSources),
        "G6 common foundation sources must remain visible to architecture checks");
  }

  @Test
  void commonFoundationMustNotImportFeatureModules() throws IOException {
    Path commonSources =
        MODULES_DIRECTORY.resolve("corba-common/src/main/java/io/github/mundanej/mjo/common");
    List<Path> violations;

    try (var paths =
        Files.find(commonSources, Integer.MAX_VALUE, ArchitectureRulesTest::isJavaSourceFile)) {
      violations = paths.filter(ArchitectureRulesTest::importsFeaturePackage).toList();
    }

    assertTrue(
        violations.isEmpty(),
        () -> "corba-common foundation sources must not import feature modules: " + violations);
  }

  private static JavaClasses projectClasses() {
    return new ClassFileImporter().importPackages("io.github.mundanej.mjo");
  }

  private static Path findRepositoryRoot() {
    Path directory = Path.of("").toAbsolutePath().normalize();
    while (directory != null) {
      if (Files.isRegularFile(directory.resolve("AGENT.md"))
          && Files.isDirectory(directory.resolve("modules"))) {
        return directory;
      }
      directory = directory.getParent();
    }
    throw new IllegalStateException("Could not locate repository root from test working directory");
  }

  private static boolean isJavaSourceFile(Path path, BasicFileAttributes attributes) {
    String relativePath =
        MODULES_DIRECTORY.relativize(path.normalize()).toString().replace('\\', '/');
    return attributes.isRegularFile()
        && relativePath.contains("/src/")
        && relativePath.endsWith(".java");
  }

  private static boolean declaresLegacyOmgPackage(Path sourceFile) {
    try (Stream<String> lines = Files.lines(sourceFile)) {
      return lines.anyMatch(line -> OMG_PACKAGE_DECLARATION.matcher(line).matches());
    } catch (IOException exception) {
      throw new IllegalStateException("Could not read " + sourceFile, exception);
    }
  }

  private static boolean containsJavaSource(Path directory) throws IOException {
    try (var paths =
        Files.find(directory, Integer.MAX_VALUE, ArchitectureRulesTest::isJavaSourceFile)) {
      return paths.findAny().isPresent();
    }
  }

  private static boolean importsFeaturePackage(Path sourceFile) {
    try (Stream<String> lines = Files.lines(sourceFile)) {
      return lines.anyMatch(ArchitectureRulesTest::isFeaturePackageImport);
    } catch (IOException exception) {
      throw new IllegalStateException("Could not read " + sourceFile, exception);
    }
  }

  private static boolean isFeaturePackageImport(String line) {
    String trimmed = line.trim();
    return trimmed.startsWith("import io.github.mundanej.mjo.")
        && !trimmed.startsWith("import io.github.mundanej.mjo.common.");
  }

  private static String[] merge(String first, String second, String[]... remaining) {
    int size = 2;
    for (String[] values : remaining) {
      size += values.length;
    }

    String[] merged = new String[size];
    merged[0] = first;
    merged[1] = second;
    int index = 2;
    for (String[] values : remaining) {
      System.arraycopy(values, 0, merged, index, values.length);
      index += values.length;
    }
    return merged;
  }
}
