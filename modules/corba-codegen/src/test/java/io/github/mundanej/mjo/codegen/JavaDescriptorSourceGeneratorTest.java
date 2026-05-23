package io.github.mundanej.mjo.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.mundanej.mjo.idl.java.mapping.IdlJavaMapper;
import io.github.mundanej.mjo.idl.java.mapping.JavaMappingMode;
import io.github.mundanej.mjo.idl.parser.IdlParseResult;
import io.github.mundanej.mjo.idl.parser.IdlParser;
import io.github.mundanej.mjo.idl.semantics.IdlSemanticAnalyzer;
import io.github.mundanej.mjo.idl.semantics.IdlSemanticModel;
import io.github.mundanej.mjo.idl.semantics.IdlSemanticResult;
import io.github.mundanej.mjo.testkit.FixtureSet;
import io.github.mundanej.mjo.testkit.GoldenAssertions;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit and generated-source tests for {@link JavaDescriptorSourceGenerator}. */
@Tag("unit")
@Tag("generated-code")
final class JavaDescriptorSourceGeneratorTest {

  private static final List<String> FORBIDDEN_DESCRIPTOR_SOURCE_TOKENS =
      List.of(
          "org.omg",
          "Helper",
          "Holder",
          "Stub",
          "Skeleton",
          "POA",
          "io.github.mundanej.mjo.orb",
          "java.lang.reflect",
          "ServiceLoader",
          "ClassLoader",
          "Proxy",
          "java.lang.invoke",
          "ObjectInputStream",
          "ObjectOutputStream",
          "Externalizable",
          "Serializable",
          "System.exit",
          "ProcessBuilder",
          "Runtime.getRuntime",
          "finalize",
          "sun.",
          "jdk.internal.");

  private final IdlParser parser = new IdlParser();
  private final IdlSemanticAnalyzer analyzer = new IdlSemanticAnalyzer();
  private final IdlJavaMapper mapper = new IdlJavaMapper();
  private final JavaSourceGenerator sourceGenerator = new JavaSourceGenerator();
  private final JavaDescriptorSourceGenerator generator = new JavaDescriptorSourceGenerator();

  @TempDir private Path tempDir;

  @Test
  void generatesHelloDescriptorAndCodecGoldensInBothModes() throws Exception {
    IdlSemanticModel semanticModel =
        semanticModel("hello/hello.idl", idlFixtures().readUtf8("hello/hello.idl"));

    List<GeneratedJavaSource> legacySources =
        generator.generate(semanticModel, JavaMappingMode.LEGACY_COMPATIBILITY);
    List<GeneratedJavaSource> modernSources =
        generator.generate(semanticModel, JavaMappingMode.MODERN);
    List<GeneratedJavaSource> allSources = new ArrayList<>();
    allSources.addAll(legacySources);
    allSources.addAll(modernSources);

    assertEquals(
        List.of(
            "hello/codec/GreeterCodec.java",
            "hello/metadata/GeneratedInterfaceRepository.java",
            "hello/metadata/GreeterDescriptor.java"),
        sourcePaths(legacySources));
    assertEquals(
        List.of(
            "modern/hello/codec/GreeterCodec.java",
            "modern/hello/metadata/GeneratedInterfaceRepository.java",
            "modern/hello/metadata/GreeterDescriptor.java"),
        sourcePaths(modernSources));
    assertGolden(
        "hello legacy codec", "hello/legacy/hello/codec/GreeterCodec.java.golden", legacySources);
    assertGolden(
        "hello legacy descriptor",
        "hello/legacy/hello/metadata/GreeterDescriptor.java.golden",
        legacySources);
    assertGolden(
        "hello legacy repository",
        "hello/legacy/hello/metadata/GeneratedInterfaceRepository.java.golden",
        legacySources);
    assertGolden(
        "hello modern codec",
        "hello/modern/modern/hello/codec/GreeterCodec.java.golden",
        modernSources);
    assertGolden(
        "hello modern descriptor",
        "hello/modern/modern/hello/metadata/GreeterDescriptor.java.golden",
        modernSources);
    assertGolden(
        "hello modern repository",
        "hello/modern/modern/hello/metadata/GeneratedInterfaceRepository.java.golden",
        modernSources);
    assertNoForbiddenGeneratedSourceTokens(
        allSources.stream().map(GeneratedJavaSource::sourceText).reduce("", String::concat));
    compile(allSources);
  }

  @Test
  void generatesDescriptorsForAggregatesExceptionsRaisesAndParameterModes() throws Exception {
    IdlSemanticModel semanticModel =
        semanticModel(
            "descriptor-test.idl",
            """
            module Demo {
              enum Color { RED, GREEN };
              struct Point { long x; string label; };
              exception Bad { string reason; };
              interface Shape {
                void ping(out long count, inout Point value) raises (Bad);
                Color favorite();
              };
            };
            """);

    List<GeneratedJavaSource> descriptorSources =
        generator.generate(semanticModel, JavaMappingMode.LEGACY_COMPATIBILITY);
    List<GeneratedJavaSource> compileSources = new ArrayList<>(descriptorSources);
    compileSources.addAll(
        sourceGenerator.generate(mapper.map(semanticModel, JavaMappingMode.LEGACY_COMPATIBILITY)));

    assertEquals(
        List.of(
            "demo/codec/BadCodec.java",
            "demo/codec/ColorCodec.java",
            "demo/codec/PointCodec.java",
            "demo/codec/ShapeCodec.java",
            "demo/metadata/BadDescriptor.java",
            "demo/metadata/ColorDescriptor.java",
            "demo/metadata/GeneratedInterfaceRepository.java",
            "demo/metadata/PointDescriptor.java",
            "demo/metadata/ShapeDescriptor.java"),
        sourcePaths(descriptorSources));
    String allDescriptorSource =
        descriptorSources.stream().map(GeneratedJavaSource::sourceText).reduce("", String::concat);
    assertNoForbiddenGeneratedSourceTokens(allDescriptorSource);
    assertContains(allDescriptorSource, "IdlTypeKind.STRUCT");
    assertContains(allDescriptorSource, "new IdlFieldDescriptor(\"x\"");
    assertContains(allDescriptorSource, "List.of(\"RED\", \"GREEN\")");
    assertContains(allDescriptorSource, "RepositoryId.parse(\"IDL:Demo/Bad:1.0\")");
    assertContains(allDescriptorSource, "IdlTypeKind.VOID");
    assertContains(allDescriptorSource, "IdlParameterMode.OUT");
    assertContains(allDescriptorSource, "IdlParameterMode.INOUT");
    assertContains(allDescriptorSource, "IdlTypeKind.EXCEPTION");
    assertContains(allDescriptorSource, "\"::Demo::Bad\", \"demo.Bad\"");
    assertContains(
        allDescriptorSource,
        "new IdlTypeReference(IdlTypeKind.EXCEPTION, \"::Demo::Bad\", \"demo.Bad\", "
            + "Optional.of(RepositoryId.parse(\"IDL:Demo/Bad:1.0\")))");
    assertContains(allDescriptorSource, "StaticInterfaceRepository.of");
    assertContains(
        allDescriptorSource,
        """
        List.of(demo.metadata.ColorDescriptor.DESCRIPTOR,
                      demo.metadata.PointDescriptor.DESCRIPTOR,
                      demo.metadata.BadDescriptor.DESCRIPTOR,
                      demo.metadata.ShapeDescriptor.DESCRIPTOR)""");
    compile(compileSources);
  }

  @Test
  void rendersRepositoryInCommonMetadataPackageForMixedDeclarationPackages() throws Exception {
    IdlSemanticModel semanticModel =
        semanticModel(
            "mixed-packages.idl",
            """
            module Gamma {
              struct Global { long id; };
            };
            module Alpha {
              struct A { string label; };
            };
            module Beta {
              interface B {
                long getA(in long global);
              };
            };
            """);

    List<GeneratedJavaSource> descriptorSources =
        generator.generate(semanticModel, JavaMappingMode.LEGACY_COMPATIBILITY);
    List<GeneratedJavaSource> compileSources = new ArrayList<>(descriptorSources);
    compileSources.addAll(
        sourceGenerator.generate(mapper.map(semanticModel, JavaMappingMode.LEGACY_COMPATIBILITY)));

    assertEquals(
        List.of(
            "alpha/codec/ACodec.java",
            "alpha/metadata/ADescriptor.java",
            "beta/codec/BCodec.java",
            "beta/metadata/BDescriptor.java",
            "gamma/codec/GlobalCodec.java",
            "gamma/metadata/GlobalDescriptor.java",
            "metadata/GeneratedInterfaceRepository.java"),
        sourcePaths(descriptorSources));
    String repositorySource =
        descriptorSources.stream()
            .filter(
                source -> source.sourcePath().equals("metadata/GeneratedInterfaceRepository.java"))
            .findFirst()
            .orElseThrow()
            .sourceText();
    assertContains(repositorySource, "package metadata;");
    assertContains(
        repositorySource,
        """
        List.of(gamma.metadata.GlobalDescriptor.DESCRIPTOR,
                      alpha.metadata.ADescriptor.DESCRIPTOR,
                      beta.metadata.BDescriptor.DESCRIPTOR)""");
    String descriptorText =
        descriptorSources.stream().map(GeneratedJavaSource::sourceText).reduce("", String::concat);
    assertContains(descriptorText, "RepositoryId.parse(\"IDL:Gamma/Global:1.0\")");
    assertContains(descriptorText, "RepositoryId.parse(\"IDL:Beta/B:1.0\")");
    assertContains(descriptorText, "RepositoryId.parse(\"IDL:Alpha/A:1.0\")");
    assertContains(descriptorText, "public static final IdlCodec<java.lang.Object> GET_A_REQUEST");
    assertContains(descriptorText, "public static final IdlCodec<java.lang.Object> GET_A_REPLY");
    assertContains(descriptorText, "public static final IdlCodec<gamma.Global> VALUE");
    compile(compileSources);
  }

  @Test
  void generatedRmiIdlFixtureMapsThroughDescriptorAndCodegenPaths() throws Exception {
    String idl = Files.readString(rmiGeneratedIdlFixture(), StandardCharsets.UTF_8);
    IdlSemanticModel semanticModel = semanticModel("rmi-generated-idl/calculator.idl", idl);

    List<GeneratedJavaSource> descriptorSources =
        generator.generate(semanticModel, JavaMappingMode.LEGACY_COMPATIBILITY);
    List<GeneratedJavaSource> compileSources = new ArrayList<>(descriptorSources);
    compileSources.addAll(
        sourceGenerator.generate(mapper.map(semanticModel, JavaMappingMode.LEGACY_COMPATIBILITY)));

    assertEquals(
        List.of(
            "example/calc/codec/CalculatorCodec.java",
            "example/calc/codec/CalculatorProblemCodec.java",
            "example/calc/metadata/CalculatorDescriptor.java",
            "example/calc/metadata/CalculatorProblemDescriptor.java",
            "example/calc/metadata/GeneratedInterfaceRepository.java"),
        sourcePaths(descriptorSources));
    String descriptorText =
        descriptorSources.stream().map(GeneratedJavaSource::sourceText).reduce("", String::concat);
    assertNoForbiddenGeneratedSourceTokens(descriptorText);
    assertContains(descriptorText, "RepositoryId.parse(\"IDL:example/calc/Calculator:1.0\")");
    assertContains(
        descriptorText, "RepositoryId.parse(\"IDL:example/calc/CalculatorProblem:1.0\")");
    compile(compileSources);
  }

  private IdlSemanticModel semanticModel(String sourceName, String source) {
    IdlParseResult parseResult = parser.parse(sourceName, source);
    assertFalse(parseResult.hasErrors(), () -> parseResult.diagnostics().toString());
    IdlSemanticResult semanticResult =
        analyzer.analyze(parseResult.translationUnit().orElseThrow());
    assertFalse(semanticResult.hasErrors(), () -> semanticResult.diagnostics().toString());
    return semanticResult.model().orElseThrow();
  }

  private void assertGolden(
      String description, String goldenPath, List<GeneratedJavaSource> sources) throws Exception {
    GoldenAssertions.assertTextEquals(
        description, goldenFixtures().readUtf8(goldenPath), sourceText(sources, goldenPath));
  }

  private static String sourceText(List<GeneratedJavaSource> sources, String goldenPath) {
    String withoutFixtureName = goldenPath.substring(goldenPath.indexOf('/') + 1);
    String sourcePath = withoutFixtureName.substring(withoutFixtureName.indexOf('/') + 1);
    if (sourcePath.endsWith(".golden")) {
      sourcePath = sourcePath.substring(0, sourcePath.length() - ".golden".length());
    }
    String generatedSourcePath = sourcePath;
    return sources.stream()
        .filter(source -> source.sourcePath().equals(generatedSourcePath))
        .findFirst()
        .orElseThrow()
        .sourceText();
  }

  private static List<String> sourcePaths(List<GeneratedJavaSource> sources) {
    return sources.stream().map(GeneratedJavaSource::sourcePath).toList();
  }

  private static void assertNoForbiddenGeneratedSourceTokens(String sourceText) {
    List<String> violations =
        FORBIDDEN_DESCRIPTOR_SOURCE_TOKENS.stream().filter(sourceText::contains).toList();

    assertEquals(List.of(), violations, "Generated source contains forbidden architecture tokens");
  }

  private static void assertContains(String text, String expected) {
    assertEquals(true, text.contains(expected), () -> "Missing generated text: " + expected);
  }

  private static FixtureSet idlFixtures() {
    return new FixtureSet(findRepositoryRoot().resolve("interop/idl"));
  }

  private static FixtureSet goldenFixtures() throws Exception {
    URL resource = JavaDescriptorSourceGeneratorTest.class.getResource("/golden-source");
    assertNotNull(resource, "golden-source test resource root");
    return new FixtureSet(Path.of(resource.toURI()));
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

  private static Path rmiGeneratedIdlFixture() {
    return findRepositoryRoot()
        .resolve("modules/corba-rmi-iiop/src/test/resources/rmi-generated-idl/calculator.idl");
  }

  private void compile(List<GeneratedJavaSource> sources) throws Exception {
    Path sourceRoot = Files.createDirectories(tempDir.resolve("generated-descriptor-src"));
    Path classOutput = Files.createDirectories(tempDir.resolve("generated-descriptor-classes"));
    List<String> arguments = new ArrayList<>();
    arguments.add("-classpath");
    arguments.add(System.getProperty("java.class.path"));
    arguments.add("-d");
    arguments.add(classOutput.toString());
    for (GeneratedJavaSource source : sources) {
      Path sourceFile = sourceRoot.resolve(source.sourcePath());
      Path parent = sourceFile.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      Files.writeString(sourceFile, source.sourceText(), StandardCharsets.UTF_8);
      arguments.add(sourceFile.toString());
    }

    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    assertNotNull(compiler, "Generated-source compilation requires a JDK compiler");
    assertEquals(0, compiler.run(null, null, null, arguments.toArray(String[]::new)));
  }
}
