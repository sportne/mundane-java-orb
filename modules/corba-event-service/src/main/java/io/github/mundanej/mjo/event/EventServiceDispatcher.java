package io.github.mundanej.mjo.event;

import io.github.mundanej.mjo.modern.LocalInvocationDispatcher;
import io.github.mundanej.mjo.modern.LocalInvocationRequest;
import io.github.mundanej.mjo.typecode.IdlOperationDescriptor;
import java.util.Objects;

final class EventServiceDispatcher implements LocalInvocationDispatcher {

  private final NetworkEventServiceState state;
  private final Object target;

  EventServiceDispatcher(NetworkEventServiceState state, Object target) {
    this.state = Objects.requireNonNull(state, "state");
    this.target = Objects.requireNonNull(target, "target");
  }

  @Override
  public Object invoke(LocalInvocationRequest request) {
    try {
      IdlOperationDescriptor operation = request.operation();
      if (target instanceof LocalEventChannel channel) {
        return invokeChannel(channel, operation);
      }
      if (target instanceof LocalEventSupplierAdmin supplierAdmin) {
        return invokeSupplierAdmin(supplierAdmin, operation);
      }
      if (target instanceof LocalEventConsumerAdmin consumerAdmin) {
        return invokeConsumerAdmin(consumerAdmin, operation);
      }
      if (target instanceof LocalPushConsumerProxy proxy) {
        return invokePushConsumer(proxy, operation, request.arguments());
      }
      if (target instanceof LocalPullConsumerProxy proxy) {
        return invokePullConsumer(proxy, operation);
      }
      if (target instanceof LocalPushSupplierProxy proxy) {
        return invokePushSupplier(proxy, operation);
      }
      if (target instanceof LocalPullSupplierProxy proxy) {
        return invokePullSupplier(proxy, operation);
      }
      throw EventServiceCorbaExceptions.badOperation(
          "Unsupported Event Service target: " + target.getClass().getName());
    } catch (EventServiceException exception) {
      throw EventServiceCorbaExceptions.from(exception);
    }
  }

  private Object invokeChannel(LocalEventChannel channel, IdlOperationDescriptor operation) {
    if (operation.equals(EventServiceDescriptors.FOR_SUPPLIERS)) {
      return state.ior(state.supplierAdminReference());
    }
    if (operation.equals(EventServiceDescriptors.FOR_CONSUMERS)) {
      return state.ior(state.consumerAdminReference());
    }
    if (operation.equals(EventServiceDescriptors.DESTROY)) {
      channel.destroy();
      return null;
    }
    throw unsupported(operation);
  }

  private Object invokeSupplierAdmin(
      LocalEventSupplierAdmin supplierAdmin, IdlOperationDescriptor operation) {
    if (operation.equals(EventServiceDescriptors.OBTAIN_PUSH_CONSUMER)) {
      LocalPushConsumerProxy proxy = supplierAdmin.obtainPushConsumerProxy();
      proxy.connectPushSupplier(() -> {});
      return state.bindProxy(proxy);
    }
    if (operation.equals(EventServiceDescriptors.OBTAIN_PULL_CONSUMER)) {
      LocalPullConsumerProxy proxy = supplierAdmin.obtainPullConsumerProxy();
      proxy.connectPullSupplier(new EmptyPullSupplier());
      return state.bindProxy(proxy);
    }
    throw unsupported(operation);
  }

  private Object invokeConsumerAdmin(
      LocalEventConsumerAdmin consumerAdmin, IdlOperationDescriptor operation) {
    if (operation.equals(EventServiceDescriptors.OBTAIN_PUSH_SUPPLIER)) {
      LocalPushSupplierProxy proxy = consumerAdmin.obtainPushSupplierProxy();
      proxy.connectPushConsumer(new NoopPushConsumer());
      return state.bindProxy(proxy);
    }
    if (operation.equals(EventServiceDescriptors.OBTAIN_PULL_SUPPLIER)) {
      LocalPullSupplierProxy proxy = consumerAdmin.obtainPullSupplierProxy();
      proxy.connectPullConsumer(() -> {});
      return state.bindProxy(proxy);
    }
    throw unsupported(operation);
  }

  private Object invokePushConsumer(
      LocalPushConsumerProxy proxy, IdlOperationDescriptor operation, java.util.List<Object> args) {
    if (operation.equals(EventServiceDescriptors.PUSH)) {
      proxy.push((io.github.mundanej.mjo.any.AnyValue<?>) args.get(0));
      return null;
    }
    if (operation.equals(EventServiceDescriptors.DISCONNECT_PUSH_CONSUMER)) {
      proxy.disconnectPushSupplier();
      return null;
    }
    throw unsupported(operation);
  }

  private Object invokePullConsumer(
      LocalPullConsumerProxy proxy, IdlOperationDescriptor operation) {
    if (operation.equals(EventServiceDescriptors.DISCONNECT_PULL_CONSUMER)) {
      proxy.disconnectPullSupplier();
      return null;
    }
    throw unsupported(operation);
  }

  private Object invokePushSupplier(
      LocalPushSupplierProxy proxy, IdlOperationDescriptor operation) {
    if (operation.equals(EventServiceDescriptors.DISCONNECT_PUSH_SUPPLIER)) {
      proxy.disconnectPushConsumer();
      return null;
    }
    throw unsupported(operation);
  }

  private Object invokePullSupplier(
      LocalPullSupplierProxy proxy, IdlOperationDescriptor operation) {
    if (operation.equals(EventServiceDescriptors.PULL)) {
      return proxy.pull();
    }
    if (operation.equals(EventServiceDescriptors.TRY_PULL)) {
      return proxy.tryPull().map(EventTryPullResult::present).orElseGet(EventTryPullResult::empty);
    }
    if (operation.equals(EventServiceDescriptors.DISCONNECT_PULL_SUPPLIER)) {
      proxy.disconnectPullConsumer();
      return null;
    }
    throw unsupported(operation);
  }

  private static org.omg.CORBA.BAD_OPERATION unsupported(IdlOperationDescriptor operation) {
    return EventServiceCorbaExceptions.badOperation(
        "Unsupported Event Service operation: " + operation.name());
  }

  private static final class NoopPushConsumer implements EventPushConsumer {
    @Override
    public void push(io.github.mundanej.mjo.any.AnyValue<?> event) {
      LocalEventChannel.requirePayload(event);
    }

    @Override
    public void disconnectPushConsumer() {}
  }

  private static final class EmptyPullSupplier implements EventPullSupplier {
    @Override
    public java.util.Optional<io.github.mundanej.mjo.any.AnyValue<?>> pull() {
      return java.util.Optional.empty();
    }

    @Override
    public java.util.Optional<io.github.mundanej.mjo.any.AnyValue<?>> tryPull() {
      return java.util.Optional.empty();
    }

    @Override
    public void disconnectPullSupplier() {}
  }
}
