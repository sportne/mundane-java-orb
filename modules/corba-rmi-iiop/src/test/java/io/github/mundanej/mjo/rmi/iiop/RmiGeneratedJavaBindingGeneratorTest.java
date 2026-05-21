package io.github.mundanej.mjo.rmi.iiop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjo.common.Diagnostic;
import io.github.mundanej.mjo.common.DiagnosticCode;
import io.github.mundanej.mjo.common.DiagnosticSeverity;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for {@link RmiGeneratedJavaBindingGenerator}. */
@Tag("unit")
@Tag("generated-code")
final class RmiGeneratedJavaBindingGeneratorTest {

  private static final RmiJavaTypeReference REMOTE_EXCEPTION =
      RmiJavaTypeReference.declared("java.rmi.RemoteException");
  private static final List<String> FORBIDDEN_RUNTIME_TOKENS =
      List.of(
          "Class.forName",
          "java.lang.reflect",
          "Proxy.newProxyInstance",
          "ObjectInputStream",
          "ObjectOutputStream",
          "java.io.Serializable",
          "ServiceLoader",
          "ClassLoader",
          "Socket",
          "ServerSocket");

  private final RmiJavaToIdlMapper mapper = new RmiJavaToIdlMapper();
  private final RmiGeneratedJavaBindingGenerator generator = new RmiGeneratedJavaBindingGenerator();

  @TempDir private Path tempDir;

  @Test
  void generatesDeterministicCompileSafeBindingSources() throws Exception {
    RmiIdlTranslationUnit translationUnit = approvedTranslationUnit();
    RmiRepositoryIdPlan repositoryIdPlan = approvedRepositoryIdPlan();

    RmiGeneratedJavaBindingResult result = generator.generate(translationUnit, repositoryIdPlan);
    RmiGeneratedJavaBindingResult repeated = generator.generate(translationUnit, repositoryIdPlan);

    assertFalse(result.hasErrors(), () -> result.diagnostics().toString());
    assertEquals(List.of(), result.diagnostics());
    assertEquals(result, repeated);
    assertEquals(
        List.of(
            "example/calc/Calculator.java",
            "example/calc/CalculatorBindingDescriptor.java",
            "example/calc/CalculatorHelper.java",
            "example/calc/CalculatorHolder.java",
            "example/calc/CalculatorProblem.java",
            "example/calc/CalculatorProblemHolder.java",
            "example/calc/CalculatorSkeleton.java",
            "example/calc/CalculatorStub.java",
            "example/calc/CalculatorTie.java"),
        result.sources().stream().map(RmiGeneratedJavaBindingSource::sourcePath).toList());
    assertEquals(goldenCalculatorInterface(), sourceText(result, "example/calc/Calculator.java"));
    assertTrue(
        sourceText(result, "example/calc/CalculatorHelper.java")
            .contains("RMI:example.calc.Calculator:0123456789ABCDEF"));
    assertTrue(
        sourceText(result, "example/calc/CalculatorProblem.java")
            .contains("RMI:example.calc.CalculatorProblem:2222222222222222"));
    assertGeneratedLocalAdapterSource(result);
    assertGeneratedWireAdapterSource(result);
    assertNoForbiddenRuntimeTokens(result);
    compile(result.sources());
  }

  @Test
  void generatedLocalAdaptersInvokeThroughOrbAndPoa() throws Exception {
    RmiGeneratedJavaBindingResult result =
        generator.generate(approvedTranslationUnit(), approvedRepositoryIdPlan());
    List<RmiGeneratedJavaBindingSource> sources = new ArrayList<>(result.sources());
    sources.add(
        new RmiGeneratedJavaBindingSource(
            "example.calc", "CalculatorLocalSmoke", localSmokeSource()));

    Path classOutput = compile(sources);

    runSmoke(classOutput, "example.calc.CalculatorLocalSmoke");
  }

  @Test
  void generatedWireAdaptersInvokeThroughIiopLoopback() throws Exception {
    RmiGeneratedJavaBindingResult result =
        generator.generate(approvedTranslationUnit(), approvedRepositoryIdPlan());
    List<RmiGeneratedJavaBindingSource> sources = new ArrayList<>(result.sources());
    sources.add(
        new RmiGeneratedJavaBindingSource(
            "example.calc", "CalculatorWireSmoke", wireSmokeSource()));

    Path classOutput = compile(sources);

    runSmoke(classOutput, "example.calc.CalculatorWireSmoke");
  }

  @Test
  void reportsUnsupportedBindingInputsInModelOrder() {
    RmiIdlTranslationUnit translationUnit =
        new RmiIdlTranslationUnit(
            List.of(),
            List.of(
                new RmiIdlInterface(
                    "Unsupported",
                    "::Unsupported",
                    Optional.of("example.Unsupported"),
                    List.of(
                        new RmiIdlOperation(
                            "bad",
                            RmiIdlTypeReference.sequenceOf(RmiIdlTypeReference.builtin("long")),
                            List.of(
                                new RmiIdlParameter(
                                    "value",
                                    RmiIdlTypeReference.declaredValue(
                                        "::example::Value", "example.Value"))),
                            List.of(
                                new RmiIdlExceptionReference(
                                    "other.Problem", "::other::Problem")))))));

    RmiGeneratedJavaBindingResult result =
        generator.generate(
            translationUnit,
            new RmiRepositoryIdPlan(
                List.of(
                    new RmiRepositoryIdValue(
                        "example.Unsupported", "RMI:example.Unsupported:0123456789ABCDEF"))));

    assertTrue(result.hasErrors());
    assertEquals(List.of(), result.sources());
    assertEquals(
        List.of(
            RmiJavaDiagnosticCodes.UNSUPPORTED_BINDING_SEQUENCE,
            RmiJavaDiagnosticCodes.UNSUPPORTED_BINDING_DECLARED_TYPE,
            RmiJavaDiagnosticCodes.UNSUPPORTED_BINDING_EXCEPTION_SCOPE,
            RmiJavaDiagnosticCodes.MISSING_BINDING_REPOSITORY_ID),
        diagnosticCodes(result));
  }

  @Test
  void reportsMissingRepositoryIdsAndDuplicateSourcePaths() {
    RmiIdlTranslationUnit missingRepositoryId =
        new RmiIdlTranslationUnit(
            List.of(),
            List.of(
                new RmiIdlInterface(
                    "Missing", "::Missing", Optional.of("example.Missing"), List.of())));
    RmiIdlTranslationUnit duplicateSourcePath =
        new RmiIdlTranslationUnit(
            List.of(),
            List.of(
                new RmiIdlInterface(
                    "Duplicate",
                    "::Duplicate",
                    Optional.of("example.Duplicate"),
                    List.of(
                        new RmiIdlOperation(
                            "bad",
                            RmiIdlTypeReference.voidType(),
                            List.of(),
                            List.of(
                                new RmiIdlExceptionReference(
                                    "example.DuplicateProblem", "::Duplicate")))))));

    assertEquals(
        List.of(RmiJavaDiagnosticCodes.MISSING_BINDING_REPOSITORY_ID),
        diagnosticCodes(
            generator.generate(missingRepositoryId, new RmiRepositoryIdPlan(List.of()))));
    assertEquals(
        List.of(
            RmiJavaDiagnosticCodes.DUPLICATE_BINDING_SOURCE_PATH,
            RmiJavaDiagnosticCodes.DUPLICATE_BINDING_SOURCE_PATH),
        diagnosticCodes(
            generator.generate(
                duplicateSourcePath,
                new RmiRepositoryIdPlan(
                    List.of(
                        new RmiRepositoryIdValue(
                            "example.Duplicate", "RMI:example.Duplicate:0123456789ABCDEF"),
                        new RmiRepositoryIdValue(
                            "example.DuplicateProblem",
                            "RMI:example.DuplicateProblem:2222222222222222"))))));
  }

  @Test
  void exposesImmutableBindingValues() {
    RmiGeneratedJavaBindingResult result =
        generator.generate(approvedTranslationUnit(), approvedRepositoryIdPlan());
    RmiGeneratedJavaBindingSource source = result.sources().getFirst();

    assertThrows(UnsupportedOperationException.class, () -> result.sources().clear());
    assertThrows(UnsupportedOperationException.class, () -> result.diagnostics().clear());
    assertThrows(
        IllegalArgumentException.class,
        () -> new RmiGeneratedJavaBindingSource("example", "Thing", "class Thing {}"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new RmiGeneratedJavaBindingResult(List.of(source), resultWithErrorCodes()));
  }

  @Test
  void keepsGeneratedBindingDiagnosticCodeValuesStable() {
    assertEquals(
        List.of("RMI-0500", "RMI-0501", "RMI-0502", "RMI-0503", "RMI-0504"),
        List.of(
                RmiJavaDiagnosticCodes.MISSING_BINDING_REPOSITORY_ID,
                RmiJavaDiagnosticCodes.UNSUPPORTED_BINDING_SEQUENCE,
                RmiJavaDiagnosticCodes.UNSUPPORTED_BINDING_DECLARED_TYPE,
                RmiJavaDiagnosticCodes.UNSUPPORTED_BINDING_EXCEPTION_SCOPE,
                RmiJavaDiagnosticCodes.DUPLICATE_BINDING_SOURCE_PATH)
            .stream()
            .map(DiagnosticCode::value)
            .toList());
  }

  @Test
  void mainSourcesAvoidForbiddenRuntimeMechanisms() throws Exception {
    Path sourceRoot = Path.of("src/main/java");
    try (Stream<Path> paths = Files.walk(sourceRoot)) {
      String sources =
          paths
              .filter(path -> path.toString().endsWith(".java"))
              .map(RmiGeneratedJavaBindingGeneratorTest::readString)
              .reduce("", String::concat);

      assertEquals(
          List.of(),
          FORBIDDEN_RUNTIME_TOKENS.stream().filter(sources::contains).toList(),
          "RMI-IIOP main sources contain forbidden runtime mechanisms");
    }
  }

  private RmiIdlTranslationUnit approvedTranslationUnit() {
    RmiJavaRemoteInterface declaration =
        new RmiJavaRemoteInterface(
            "example.calc.Calculator",
            true,
            List.of(
                RmiJavaOperation.abstractOperation(
                    "add",
                    RmiJavaTypeReference.primitive("int"),
                    List.of(
                        new RmiJavaParameter("left", RmiJavaTypeReference.primitive("int")),
                        new RmiJavaParameter("right", RmiJavaTypeReference.primitive("int"))),
                    List.of(REMOTE_EXCEPTION)),
                RmiJavaOperation.abstractOperation(
                    "describe",
                    RmiJavaTypeReference.declared("java.lang.String"),
                    List.of(
                        new RmiJavaParameter(
                            "name", RmiJavaTypeReference.declared("java.lang.String"))),
                    List.of(
                        RmiJavaTypeReference.declared("example.calc.CalculatorProblem"),
                        REMOTE_EXCEPTION)),
                RmiJavaOperation.abstractOperation(
                    "clear",
                    RmiJavaTypeReference.voidType(),
                    List.of(),
                    List.of(REMOTE_EXCEPTION))));

    RmiJavaToIdlResult result = mapper.map(declaration);

    assertFalse(result.hasErrors(), () -> result.diagnostics().toString());
    return result.translationUnit().orElseThrow();
  }

  private static RmiRepositoryIdPlan approvedRepositoryIdPlan() {
    return new RmiRepositoryIdPlan(
        List.of(
            new RmiRepositoryIdValue(
                "example.calc.Calculator", "RMI:example.calc.Calculator:0123456789ABCDEF"),
            new RmiRepositoryIdValue(
                "example.calc.CalculatorProblem",
                "RMI:example.calc.CalculatorProblem:2222222222222222")));
  }

  private static String goldenCalculatorInterface() {
    return """
        // Generated by mundane-java-orb G7-080.
        // Compatibility profile: local and wire RMI-IIOP binding surface.

        package example.calc;

        public interface Calculator extends java.rmi.Remote {

          public int add(int left, int right) throws java.rmi.RemoteException;

          public java.lang.String describe(java.lang.String name) throws java.rmi.RemoteException, CalculatorProblem;

          public void clear() throws java.rmi.RemoteException;
        }
        """;
  }

  private static String sourceText(RmiGeneratedJavaBindingResult result, String sourcePath) {
    return result.sources().stream()
        .filter(source -> source.sourcePath().equals(sourcePath))
        .findFirst()
        .orElseThrow()
        .sourceText();
  }

  private static void assertGeneratedLocalAdapterSource(RmiGeneratedJavaBindingResult result) {
    String descriptor = sourceText(result, "example/calc/CalculatorBindingDescriptor.java");
    String stub = sourceText(result, "example/calc/CalculatorStub.java");
    String tie = sourceText(result, "example/calc/CalculatorTie.java");
    String skeleton = sourceText(result, "example/calc/CalculatorSkeleton.java");

    assertTrue(
        descriptor.contains(
            "public static final io.github.mundanej.mjo.typecode.IdlGeneratedTypeDescriptor DESCRIPTOR"));
    assertTrue(
        descriptor.contains(
            "public static final io.github.mundanej.mjo.typecode.IdlOperationDescriptor ADD_OPERATION"));
    assertTrue(
        descriptor.contains(
            "public static final io.github.mundanej.mjo.typecode.IdlTypeReference CALCULATOR_PROBLEM_EXCEPTION_TYPE"));
    assertTrue(stub.contains("public CalculatorStub(io.github.mundanej.mjo.orb.LocalOrb orb,"));
    assertTrue(stub.contains("orb.invoke(reference, CalculatorBindingDescriptor.ADD_OPERATION"));
    assertTrue(
        stub.contains("exception.userException() instanceof CalculatorProblem typedException"));
    assertTrue(tie.contains("activate(io.github.mundanej.mjo.poa.Poa poa)"));
    assertTrue(tie.contains(".activateServant("));
    assertTrue(
        tie.contains("CalculatorBindingDescriptor.ADD_OPERATION.equals(request.operation())"));
    assertTrue(skeleton.contains("return new CalculatorTie(this).activate(poa);"));
  }

  private static void assertGeneratedWireAdapterSource(RmiGeneratedJavaBindingResult result) {
    String descriptor = sourceText(result, "example/calc/CalculatorBindingDescriptor.java");
    String stub = sourceText(result, "example/calc/CalculatorStub.java");

    assertTrue(
        descriptor.contains(
            "public static final io.github.mundanej.mjo.rmi.iiop.RmiIdlInterface RMI_INTERFACE"));
    assertTrue(
        descriptor.contains(
            "public static final io.github.mundanej.mjo.rmi.iiop.RmiIdlOperation ADD_OPERATION_RMI_MODEL"));
    assertTrue(
        stub.contains(
            "public CalculatorStub(io.github.mundanej.mjo.rmi.iiop.RmiIiopWireClient wireClient,"));
    assertTrue(
        stub.contains(
            "wireClient.invoke(objectKey, CalculatorBindingDescriptor.ADD_OPERATION_RMI_MODEL"));
    assertTrue(stub.contains("CalculatorProblem.REPOSITORY_ID.equals(exception.repositoryId())"));
  }

  private static List<DiagnosticCode> diagnosticCodes(RmiGeneratedJavaBindingResult result) {
    assertTrue(
        result.diagnostics().stream()
            .allMatch(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR));
    return result.diagnostics().stream().map(Diagnostic::code).toList();
  }

  private static List<Diagnostic> resultWithErrorCodes() {
    return List.of(
        Diagnostic.withoutSpan(
            RmiJavaDiagnosticCodes.MISSING_BINDING_REPOSITORY_ID,
            DiagnosticSeverity.ERROR,
            "error"));
  }

  private static void assertNoForbiddenRuntimeTokens(RmiGeneratedJavaBindingResult result) {
    String sourceText =
        result.sources().stream()
            .map(RmiGeneratedJavaBindingSource::sourceText)
            .reduce("", String::concat);
    assertEquals(
        List.of(),
        FORBIDDEN_RUNTIME_TOKENS.stream().filter(sourceText::contains).toList(),
        "Generated binding source contains forbidden runtime mechanisms");
  }

  private Path compile(List<RmiGeneratedJavaBindingSource> sources) throws Exception {
    Path sourceRoot = Files.createDirectories(tempDir.resolve("generated-rmi-src"));
    Path classOutput = Files.createDirectories(tempDir.resolve("generated-rmi-classes"));
    List<String> arguments = new ArrayList<>();
    arguments.add("-classpath");
    arguments.add(System.getProperty("java.class.path"));
    arguments.add("-d");
    arguments.add(classOutput.toString());
    for (RmiGeneratedJavaBindingSource source : sources) {
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
    return classOutput;
  }

  private void runSmoke(Path classOutput, String className) throws Exception {
    try (URLClassLoader loader =
        new URLClassLoader(new URL[] {classOutput.toUri().toURL()}, getClass().getClassLoader())) {
      Class<?> smoke = loader.loadClass(className);
      Method method = smoke.getDeclaredMethod("run");
      method.invoke(null);
    } catch (InvocationTargetException exception) {
      Throwable cause = exception.getCause();
      if (cause instanceof Exception checked) {
        throw checked;
      }
      if (cause instanceof Error error) {
        throw error;
      }
      throw exception;
    }
  }

  private static String localSmokeSource() {
    return """
        // Generated test harness for G7-070 local adapter behavior.

        package example.calc;

        public final class CalculatorLocalSmoke {

          private CalculatorLocalSmoke() {}

          public static void run() throws Exception {
            io.github.mundanej.mjo.orb.LocalOrb orb = io.github.mundanej.mjo.orb.LocalOrb.create();
            io.github.mundanej.mjo.poa.Poa poa = io.github.mundanej.mjo.poa.Poa.createRoot(orb);
            CalculatorSkeleton servant =
                new CalculatorSkeleton() {
                  @Override
                  public int add(int left, int right) throws java.rmi.RemoteException {
                    return left + right;
                  }

                  @Override
                  public java.lang.String describe(java.lang.String name)
                      throws java.rmi.RemoteException, CalculatorProblem {
                    if ("bad".equals(name)) {
                      throw new CalculatorProblem("bad");
                    }
                    return "Calculator " + name;
                  }

                  @Override
                  public void clear() throws java.rmi.RemoteException {}
                };

            io.github.mundanej.mjo.orb.LocalObjectReference<Calculator> reference =
                new CalculatorTie(servant).activate(poa);
            Calculator client = new CalculatorStub(orb, reference);

            require(client.add(13, 29) == 42, "add result");
            require("Calculator Ada".equals(client.describe("Ada")), "describe result");
            client.clear();
            try {
              client.describe("bad");
              throw new AssertionError("declared user exception was not propagated");
            } catch (CalculatorProblem expected) {
              require("bad".equals(expected.getMessage()), "user exception message");
            }

            CalculatorSkeleton otherServant =
                new CalculatorSkeleton() {
                  @Override
                  public int add(int left, int right) throws java.rmi.RemoteException {
                    return left + right;
                  }

                  @Override
                  public java.lang.String describe(java.lang.String name)
                      throws java.rmi.RemoteException, CalculatorProblem {
                    return "Other " + name;
                  }

                  @Override
                  public void clear() throws java.rmi.RemoteException {}
                };
            io.github.mundanej.mjo.orb.LocalObjectReference<Calculator> skeletonReference =
                otherServant.activate(poa);
            Calculator skeletonClient = new CalculatorStub(orb, skeletonReference);
            require(skeletonClient.add(1, 2) == 3, "skeleton activation");

            expectBadOperation(orb, reference);
            expectBadParam(orb, reference);
            poa.deactivateObject(reference.objectId());
            expectRemoteException(() -> client.add(1, 2), "deactivated reference");
            orb.shutdown();
            expectRemoteException(() -> skeletonClient.add(1, 2), "shutdown reference");
          }

          private static void expectBadOperation(
              io.github.mundanej.mjo.orb.LocalOrb orb,
              io.github.mundanej.mjo.orb.LocalObjectReference<Calculator> reference) {
            try {
              orb.invoke(
                  reference,
                  new io.github.mundanej.mjo.typecode.IdlOperationDescriptor(
                      "missing",
                      CalculatorBindingDescriptor.VOID_TYPE,
                      java.util.List.of(),
                      java.util.List.of()),
                  java.util.List.of());
              throw new AssertionError("unsupported operation was not rejected");
            } catch (org.omg.CORBA.BAD_OPERATION expected) {
              // expected
            }
          }

          private static void expectBadParam(
              io.github.mundanej.mjo.orb.LocalOrb orb,
              io.github.mundanej.mjo.orb.LocalObjectReference<Calculator> reference) {
            try {
              orb.invoke(reference, CalculatorBindingDescriptor.ADD_OPERATION, java.util.List.of(1));
              throw new AssertionError("wrong argument count was not rejected");
            } catch (org.omg.CORBA.BAD_PARAM expected) {
              // expected
            }
          }

          private static void expectRemoteException(ThrowingRemoteCall call, String label)
              throws Exception {
            try {
              call.run();
              throw new AssertionError(label + " did not fail");
            } catch (java.rmi.RemoteException expected) {
              // expected
            }
          }

          private static void require(boolean condition, String label) {
            if (!condition) {
              throw new AssertionError(label);
            }
          }

          @FunctionalInterface
          private interface ThrowingRemoteCall {

            void run() throws Exception;
          }
        }
        """;
  }

  private static String wireSmokeSource() {
    return """
        // Generated test harness for G7-080 wire adapter behavior.

        package example.calc;

        public final class CalculatorWireSmoke {

          private CalculatorWireSmoke() {}

          public static void run() throws Exception {
            io.github.mundanej.mjo.orb.LocalOrb orb = io.github.mundanej.mjo.orb.LocalOrb.create();
            io.github.mundanej.mjo.poa.Poa poa = io.github.mundanej.mjo.poa.Poa.createRoot(orb);
            CalculatorSkeleton servant =
                new CalculatorSkeleton() {
                  @Override
                  public int add(int left, int right) throws java.rmi.RemoteException {
                    return left + right;
                  }

                  @Override
                  public java.lang.String describe(java.lang.String name)
                      throws java.rmi.RemoteException, CalculatorProblem {
                    if ("bad".equals(name)) {
                      throw new CalculatorProblem("bad");
                    }
                    return "Wire " + name;
                  }

                  @Override
                  public void clear() throws java.rmi.RemoteException {}
                };

            io.github.mundanej.mjo.orb.LocalObjectReference<Calculator> reference =
                new CalculatorTie(servant).activate(poa);
            io.github.mundanej.mjo.rmi.iiop.RmiIiopObjectKey objectKey =
                io.github.mundanej.mjo.rmi.iiop.RmiIiopObjectKey.forLocalObjectReference(reference);
            io.github.mundanej.mjo.rmi.iiop.RmiIiopWireServerHandler handler =
                new io.github.mundanej.mjo.rmi.iiop.RmiIiopWireServerHandler(
                        orb, repositoryIdPlan())
                    .register(objectKey, reference, CalculatorBindingDescriptor.RMI_INTERFACE);

            try (io.github.mundanej.mjo.iiop.IiopServer server =
                    io.github.mundanej.mjo.iiop.IiopServer.bind(
                        io.github.mundanej.mjo.iiop.IiopEndpoint.loopback(0),
                        io.github.mundanej.mjo.iiop.IiopOptions.defaults(),
                        handler);
                io.github.mundanej.mjo.iiop.IiopClient iiopClient =
                    io.github.mundanej.mjo.iiop.IiopClient.connect(
                        server.endpoint(), io.github.mundanej.mjo.iiop.IiopOptions.defaults());
                io.github.mundanej.mjo.rmi.iiop.RmiIiopWireClient wireClient =
                    new io.github.mundanej.mjo.rmi.iiop.RmiIiopWireClient(
                        iiopClient, repositoryIdPlan())) {
              Calculator client = new CalculatorStub(wireClient, objectKey);
              require(client.add(13, 29) == 42, "wire add result");
              require("Wire Ada".equals(client.describe("Ada")), "wire describe result");
              client.clear();
              try {
                client.describe("bad");
                throw new AssertionError("wire user exception was not propagated");
              } catch (CalculatorProblem expected) {
                require(
                    CalculatorProblem.REPOSITORY_ID.equals(
                        "RMI:example.calc.CalculatorProblem:2222222222222222"),
                    "user exception repository ID");
              }

              Calculator missing =
                  new CalculatorStub(
                      wireClient,
                      io.github.mundanej.mjo.rmi.iiop.RmiIiopObjectKey.fromString("missing"));
              expectRemoteException(() -> missing.add(1, 2), "unknown object key");

              expectWireException(
                  () ->
                      wireClient.invoke(
                          objectKey,
                          new io.github.mundanej.mjo.rmi.iiop.RmiIdlOperation(
                              "missing",
                              io.github.mundanej.mjo.rmi.iiop.RmiIdlTypeReference.voidType(),
                              java.util.List.of(),
                              java.util.List.of()),
                          java.util.List.of()),
                  io.github.mundanej.mjo.rmi.iiop.RmiJavaDiagnosticCodes.UNKNOWN_WIRE_OPERATION);
            }
          }

          private static io.github.mundanej.mjo.rmi.iiop.RmiRepositoryIdPlan repositoryIdPlan() {
            return new io.github.mundanej.mjo.rmi.iiop.RmiRepositoryIdPlan(
                java.util.List.of(
                    new io.github.mundanej.mjo.rmi.iiop.RmiRepositoryIdValue(
                        "example.calc.Calculator",
                        "RMI:example.calc.Calculator:0123456789ABCDEF"),
                    new io.github.mundanej.mjo.rmi.iiop.RmiRepositoryIdValue(
                        "example.calc.CalculatorProblem",
                        "RMI:example.calc.CalculatorProblem:2222222222222222")));
          }

          private static void expectRemoteException(ThrowingRemoteCall call, String label)
              throws Exception {
            try {
              call.run();
              throw new AssertionError(label + " did not fail");
            } catch (java.rmi.RemoteException expected) {
              // expected
            }
          }

          private static void expectWireException(
              ThrowingRemoteCall call, io.github.mundanej.mjo.common.DiagnosticCode code)
              throws Exception {
            try {
              call.run();
              throw new AssertionError(code.value() + " did not fail");
            } catch (io.github.mundanej.mjo.rmi.iiop.RmiIiopWireException expected) {
              require(code.equals(expected.code()), "wire diagnostic code");
            }
          }

          private static void require(boolean condition, String label) {
            if (!condition) {
              throw new AssertionError(label);
            }
          }

          @FunctionalInterface
          private interface ThrowingRemoteCall {

            void run() throws Exception;
          }
        }
        """;
  }

  private static String readString(Path path) {
    try {
      return Files.readString(path);
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to read " + path, exception);
    }
  }
}
