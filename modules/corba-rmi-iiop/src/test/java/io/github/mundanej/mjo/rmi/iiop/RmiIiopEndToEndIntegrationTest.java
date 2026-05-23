package io.github.mundanej.mjo.rmi.iiop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.github.mundanej.mjo.iiop.IiopClient;
import io.github.mundanej.mjo.iiop.IiopEndpoint;
import io.github.mundanej.mjo.iiop.IiopOptions;
import io.github.mundanej.mjo.iiop.IiopServer;
import io.github.mundanej.mjo.orb.LocalObjectReference;
import io.github.mundanej.mjo.orb.LocalOrb;
import io.github.mundanej.mjo.poa.Poa;
import io.github.mundanej.mjo.repositoryid.RepositoryId;
import io.github.mundanej.mjo.typecode.IdlGeneratedTypeDescriptor;
import io.github.mundanej.mjo.typecode.IdlOperationDescriptor;
import io.github.mundanej.mjo.typecode.IdlParameterDescriptor;
import io.github.mundanej.mjo.typecode.IdlParameterMode;
import io.github.mundanej.mjo.typecode.IdlTypeKind;
import io.github.mundanej.mjo.typecode.IdlTypeReference;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Local integration tests for the approved RMI-IIOP JVM stack. */
@Tag("unit")
final class RmiIiopEndToEndIntegrationTest {

  private static final RmiJavaTypeReference REMOTE_EXCEPTION =
      RmiJavaTypeReference.declared("java.rmi.RemoteException");
  private static final IdlTypeReference LONG_TYPE =
      new IdlTypeReference(IdlTypeKind.PRIMITIVE, "long", "int", Optional.empty());
  private static final IdlOperationDescriptor ADD_DESCRIPTOR =
      new IdlOperationDescriptor(
          "add",
          LONG_TYPE,
          List.of(
              new IdlParameterDescriptor("left", IdlParameterMode.IN, LONG_TYPE),
              new IdlParameterDescriptor("right", IdlParameterMode.IN, LONG_TYPE)),
          List.of());
  private static final IdlGeneratedTypeDescriptor CALCULATOR_DESCRIPTOR =
      new IdlGeneratedTypeDescriptor(
          IdlTypeKind.INTERFACE,
          "::example::calc::Calculator",
          Calculator.class.getName(),
          RepositoryId.parse("IDL:example/calc/Calculator:1.0"),
          List.of(),
          List.of(),
          List.of(ADD_DESCRIPTOR));

  @Test
  void mappedRmiInterfaceDispatchesThroughPoaAndLoopbackIiop() throws Exception {
    RmiJavaRemoteInterface javaInterface = calculatorJavaInterface();
    RmiJavaToIdlResult mapping = new RmiJavaToIdlMapper().map(javaInterface);

    assertFalse(mapping.hasErrors(), () -> mapping.diagnostics().toString());

    RmiIdlTranslationUnit translationUnit = mapping.translationUnit().orElseThrow();
    RmiRepositoryIdPlanResult planResult =
        new RmiRepositoryIdPlanner()
            .plan(
                translationUnit,
                List.of(
                    new RmiRepositoryIdHashMetadata(
                        "example.calc.Calculator", "0123456789ABCDEF")));

    assertFalse(planResult.hasErrors(), () -> planResult.diagnostics().toString());

    RmiIdlInterface idlInterface =
        translationUnit.modules().getFirst().modules().getFirst().interfaces().getFirst();
    RmiIdlOperation addOperation = idlInterface.operations().getFirst();
    RmiRepositoryIdPlan repositoryIdPlan = planResult.plan().orElseThrow();
    LocalOrb orb = LocalOrb.create();
    Poa poa = Poa.createRoot(orb);
    CalculatorServant servant = new CalculatorServant();
    LocalObjectReference<Calculator> reference =
        poa.activateServant(
            Calculator.class,
            CALCULATOR_DESCRIPTOR,
            servant,
            (target, request) ->
                target.add(
                    (Integer) request.arguments().get(0), (Integer) request.arguments().get(1)));
    RmiIiopObjectKey objectKey = RmiIiopObjectKey.forLocalObjectReference(reference);
    RmiIiopWireServerHandler handler =
        new RmiIiopWireServerHandler(orb, repositoryIdPlan)
            .register(objectKey, reference, idlInterface);

    try (IiopServer server =
            IiopServer.bind(IiopEndpoint.loopback(0), IiopOptions.defaults(), handler);
        IiopClient iiopClient = IiopClient.connect(server.endpoint(), IiopOptions.defaults());
        RmiIiopWireClient wireClient = new RmiIiopWireClient(iiopClient, repositoryIdPlan)) {
      RmiCdrValue result =
          wireClient.invoke(
              objectKey,
              addOperation,
              List.of(RmiCdrValue.longValue(13), RmiCdrValue.longValue(29)));

      assertEquals(RmiCdrValue.longValue(42), result);
      assertEquals(List.of(13, 29), servant.seenArguments);
      assertEquals(
          "RMI:example.calc.Calculator:0123456789ABCDEF",
          repositoryIdPlan.repositoryIds().getFirst().repositoryId());
    }
  }

  private static RmiJavaRemoteInterface calculatorJavaInterface() {
    return new RmiJavaRemoteInterface(
        "example.calc.Calculator",
        true,
        List.of(
            RmiJavaOperation.abstractOperation(
                "add",
                RmiJavaTypeReference.primitive("int"),
                List.of(
                    new RmiJavaParameter("left", RmiJavaTypeReference.primitive("int")),
                    new RmiJavaParameter("right", RmiJavaTypeReference.primitive("int"))),
                List.of(REMOTE_EXCEPTION))));
  }

  @SuppressWarnings("UnusedMethod")
  private interface Calculator {

    int add(int left, int right);
  }

  private static final class CalculatorServant implements Calculator {

    private List<Integer> seenArguments = List.of();

    @Override
    public int add(int left, int right) {
      seenArguments = List.of(left, right);
      return left + right;
    }
  }
}
