package io.github.mundanej.mjo.nativeimage.smoke;

import io.github.mundanej.mjo.common.DiagnosticCode;
import io.github.mundanej.mjo.iiop.IiopClient;
import io.github.mundanej.mjo.iiop.IiopEndpoint;
import io.github.mundanej.mjo.iiop.IiopOptions;
import io.github.mundanej.mjo.iiop.IiopServer;
import io.github.mundanej.mjo.modern.LocalInvocationDispatcher;
import io.github.mundanej.mjo.orb.LocalObjectReference;
import io.github.mundanej.mjo.orb.LocalOrb;
import io.github.mundanej.mjo.repositoryid.RepositoryId;
import io.github.mundanej.mjo.rmi.iiop.RmiCdrValue;
import io.github.mundanej.mjo.rmi.iiop.RmiIdlExceptionReference;
import io.github.mundanej.mjo.rmi.iiop.RmiIdlInterface;
import io.github.mundanej.mjo.rmi.iiop.RmiIdlOperation;
import io.github.mundanej.mjo.rmi.iiop.RmiIdlParameter;
import io.github.mundanej.mjo.rmi.iiop.RmiIdlTypeReference;
import io.github.mundanej.mjo.rmi.iiop.RmiIiopObjectKey;
import io.github.mundanej.mjo.rmi.iiop.RmiIiopWireClient;
import io.github.mundanej.mjo.rmi.iiop.RmiIiopWireException;
import io.github.mundanej.mjo.rmi.iiop.RmiIiopWireServerHandler;
import io.github.mundanej.mjo.rmi.iiop.RmiIiopWireUserException;
import io.github.mundanej.mjo.rmi.iiop.RmiJavaDiagnosticCodes;
import io.github.mundanej.mjo.rmi.iiop.RmiRepositoryIdPlan;
import io.github.mundanej.mjo.rmi.iiop.RmiRepositoryIdValue;
import io.github.mundanej.mjo.typecode.IdlGeneratedTypeDescriptor;
import io.github.mundanej.mjo.typecode.IdlOperationDescriptor;
import io.github.mundanej.mjo.typecode.IdlParameterDescriptor;
import io.github.mundanej.mjo.typecode.IdlParameterMode;
import io.github.mundanej.mjo.typecode.IdlTypeKind;
import io.github.mundanej.mjo.typecode.IdlTypeReference;
import java.util.List;
import java.util.Optional;

/** Native Image smoke entry point for the approved local RMI-IIOP wire slice. */
public final class RmiIiopNativeSmoke {

  private static final String CALCULATOR_REPOSITORY_ID =
      "RMI:io.github.mundanej.mjo.nativeimage.smoke.RmiIiopNativeSmoke.Calculator:0123456789ABCDEF";
  private static final String PROBLEM_REPOSITORY_ID =
      "RMI:io.github.mundanej.mjo.nativeimage.smoke.RmiIiopNativeSmoke.CalculatorProblem:2222222222222222";
  private static final IdlTypeReference LONG_TYPE =
      new IdlTypeReference(IdlTypeKind.PRIMITIVE, "long", "int", Optional.empty());
  private static final IdlTypeReference WSTRING_TYPE =
      new IdlTypeReference(IdlTypeKind.PRIMITIVE, "wstring", "java.lang.String", Optional.empty());
  private static final IdlTypeReference PROBLEM_TYPE =
      new IdlTypeReference(
          IdlTypeKind.EXCEPTION,
          "::nativeimage::rmi::CalculatorProblem",
          CalculatorProblem.class.getName(),
          Optional.of(RepositoryId.parse(PROBLEM_REPOSITORY_ID)));
  private static final IdlOperationDescriptor ADD_DESCRIPTOR =
      new IdlOperationDescriptor(
          "add",
          LONG_TYPE,
          List.of(
              new IdlParameterDescriptor("left", IdlParameterMode.IN, LONG_TYPE),
              new IdlParameterDescriptor("right", IdlParameterMode.IN, LONG_TYPE)),
          List.of());
  private static final IdlOperationDescriptor DESCRIBE_DESCRIPTOR =
      new IdlOperationDescriptor(
          "describe",
          WSTRING_TYPE,
          List.of(new IdlParameterDescriptor("name", IdlParameterMode.IN, WSTRING_TYPE)),
          List.of(PROBLEM_TYPE));
  private static final IdlGeneratedTypeDescriptor CALCULATOR_DESCRIPTOR =
      new IdlGeneratedTypeDescriptor(
          IdlTypeKind.INTERFACE,
          "::nativeimage::rmi::Calculator",
          Calculator.class.getName(),
          RepositoryId.parse(CALCULATOR_REPOSITORY_ID),
          List.of(),
          List.of(),
          List.of(ADD_DESCRIPTOR, DESCRIBE_DESCRIPTOR));

  private RmiIiopNativeSmoke() {}

  /** Runs local JVM RMI-IIOP request/reply paths that also build as Native Image smoke code. */
  public static void main(String[] args) throws Exception {
    LocalOrb orb = LocalOrb.create();
    LocalObjectReference<Calculator> reference =
        orb.bindWithObjectId(Calculator.class, CALCULATOR_DESCRIPTOR, "native-rmi", dispatcher());
    RmiIiopObjectKey objectKey = RmiIiopObjectKey.forLocalObjectReference(reference);
    RmiIiopWireServerHandler handler =
        new RmiIiopWireServerHandler(orb, repositoryIdPlan())
            .register(objectKey, reference, rmiInterface());

    try (IiopServer server =
            IiopServer.bind(IiopEndpoint.loopback(0), IiopOptions.defaults(), handler);
        IiopClient iiopClient = IiopClient.connect(server.endpoint(), IiopOptions.defaults());
        RmiIiopWireClient wireClient = new RmiIiopWireClient(iiopClient, repositoryIdPlan())) {
      RmiCdrValue addResult =
          wireClient.invoke(
              objectKey,
              addOperation(),
              List.of(RmiCdrValue.longValue(13), RmiCdrValue.longValue(29)));
      SmokeAssertions.requireEquals(RmiCdrValue.longValue(42), addResult, "RMI-IIOP add result");

      RmiCdrValue describeResult =
          wireClient.invoke(
              objectKey, describeOperation(), List.of(RmiCdrValue.stringValue("Ada")));
      SmokeAssertions.requireEquals(
          RmiCdrValue.stringValue("Calculator Ada"), describeResult, "RMI-IIOP describe result");

      assertUserException(wireClient, objectKey);
      assertWireFailure(
          () ->
              wireClient.invoke(
                  RmiIiopObjectKey.fromString("missing"),
                  addOperation(),
                  List.of(RmiCdrValue.longValue(1), RmiCdrValue.longValue(2))),
          RmiJavaDiagnosticCodes.UNKNOWN_WIRE_OBJECT_KEY);
      assertWireFailure(
          () ->
              wireClient.invoke(
                  objectKey,
                  new RmiIdlOperation(
                      "missing", RmiIdlTypeReference.voidType(), List.of(), List.of()),
                  List.of()),
          RmiJavaDiagnosticCodes.UNKNOWN_WIRE_OPERATION);
    } finally {
      orb.shutdown();
    }
  }

  private static LocalInvocationDispatcher dispatcher() {
    return request -> {
      if (ADD_DESCRIPTOR.equals(request.operation())) {
        int left = (Integer) request.arguments().get(0);
        int right = (Integer) request.arguments().get(1);
        return left + right;
      }
      if (DESCRIBE_DESCRIPTOR.equals(request.operation())) {
        String name = (String) request.arguments().getFirst();
        if ("bad".equals(name)) {
          throw new CalculatorProblem();
        }
        return "Calculator " + name;
      }
      throw new AssertionError("Unexpected operation " + request.operation().name());
    };
  }

  private static void assertUserException(RmiIiopWireClient wireClient, RmiIiopObjectKey objectKey)
      throws Exception {
    try {
      wireClient.invoke(objectKey, describeOperation(), List.of(RmiCdrValue.stringValue("bad")));
      throw new AssertionError("RMI-IIOP user exception was not raised");
    } catch (RmiIiopWireUserException expected) {
      SmokeAssertions.requireEquals(
          PROBLEM_REPOSITORY_ID, expected.repositoryId(), "RMI-IIOP user exception repository ID");
    }
  }

  private static void assertWireFailure(ThrowingWireCall call, DiagnosticCode expectedCode)
      throws Exception {
    try {
      call.run();
      throw new AssertionError("RMI-IIOP wire failure was not raised: " + expectedCode.value());
    } catch (RmiIiopWireException expected) {
      SmokeAssertions.requireEquals(expectedCode, expected.code(), "RMI-IIOP diagnostic code");
    }
  }

  private static RmiIdlInterface rmiInterface() {
    return new RmiIdlInterface(
        "Calculator",
        "::nativeimage::rmi::Calculator",
        Optional.of(Calculator.class.getName()),
        List.of(addOperation(), describeOperation()));
  }

  private static RmiIdlOperation addOperation() {
    return new RmiIdlOperation(
        "add",
        RmiIdlTypeReference.builtin("long"),
        List.of(
            new RmiIdlParameter("left", RmiIdlTypeReference.builtin("long")),
            new RmiIdlParameter("right", RmiIdlTypeReference.builtin("long"))),
        List.of());
  }

  private static RmiIdlOperation describeOperation() {
    return new RmiIdlOperation(
        "describe",
        RmiIdlTypeReference.builtin("wstring"),
        List.of(new RmiIdlParameter("name", RmiIdlTypeReference.builtin("wstring"))),
        List.of(
            new RmiIdlExceptionReference(
                CalculatorProblem.class.getName(), "::nativeimage::rmi::CalculatorProblem")));
  }

  private static RmiRepositoryIdPlan repositoryIdPlan() {
    return new RmiRepositoryIdPlan(
        List.of(
            new RmiRepositoryIdValue(Calculator.class.getName(), CALCULATOR_REPOSITORY_ID),
            new RmiRepositoryIdValue(CalculatorProblem.class.getName(), PROBLEM_REPOSITORY_ID)));
  }

  private interface Calculator {}

  private static final class CalculatorProblem extends Exception {
    private static final long serialVersionUID = 1L;
  }

  @FunctionalInterface
  private interface ThrowingWireCall {

    void run() throws Exception;
  }
}
