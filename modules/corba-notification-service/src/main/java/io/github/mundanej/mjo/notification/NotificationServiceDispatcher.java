package io.github.mundanej.mjo.notification;

import io.github.mundanej.mjo.modern.LocalInvocationDispatcher;
import io.github.mundanej.mjo.modern.LocalInvocationRequest;
import io.github.mundanej.mjo.typecode.IdlOperationDescriptor;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

final class NotificationServiceDispatcher implements LocalInvocationDispatcher {

  private final NetworkNotificationServiceState state;
  private final Object target;

  NotificationServiceDispatcher(NetworkNotificationServiceState state, Object target) {
    this.state = Objects.requireNonNull(state, "state");
    this.target = Objects.requireNonNull(target, "target");
  }

  @Override
  public Object invoke(LocalInvocationRequest request) {
    try {
      IdlOperationDescriptor operation = request.operation();
      if (target instanceof LocalNotificationChannel channel) {
        return invokeChannel(channel, operation);
      }
      if (target instanceof LocalNotificationSupplierAdmin supplierAdmin) {
        return invokeSupplierAdmin(supplierAdmin, operation);
      }
      if (target instanceof LocalNotificationConsumerAdmin consumerAdmin) {
        return invokeConsumerAdmin(consumerAdmin, operation);
      }
      if (target instanceof LocalStructuredPushConsumerProxy proxy) {
        return invokePushConsumer(proxy, operation, request.arguments());
      }
      if (target instanceof LocalStructuredPullConsumerProxy proxy) {
        return invokePullConsumer(proxy, operation);
      }
      if (target instanceof LocalStructuredPushSupplierProxy proxy) {
        return invokePushSupplier(proxy, operation, request.arguments());
      }
      if (target instanceof LocalStructuredPullSupplierProxy proxy) {
        return invokePullSupplier(proxy, operation, request.arguments());
      }
      throw NotificationServiceCorbaExceptions.badOperation(
          "Unsupported Notification Service target: " + target.getClass().getName());
    } catch (NotificationServiceException exception) {
      throw NotificationServiceCorbaExceptions.from(exception);
    }
  }

  private Object invokeChannel(LocalNotificationChannel channel, IdlOperationDescriptor operation) {
    if (operation.equals(NotificationServiceDescriptors.FOR_SUPPLIERS)) {
      return state.ior(state.supplierAdminReference());
    }
    if (operation.equals(NotificationServiceDescriptors.FOR_CONSUMERS)) {
      return state.ior(state.consumerAdminReference());
    }
    if (operation.equals(NotificationServiceDescriptors.DESTROY)) {
      channel.destroy();
      return null;
    }
    throw unsupported(operation);
  }

  private Object invokeSupplierAdmin(
      LocalNotificationSupplierAdmin supplierAdmin, IdlOperationDescriptor operation) {
    if (operation.equals(NotificationServiceDescriptors.OBTAIN_STRUCTURED_PUSH_CONSUMER)) {
      LocalStructuredPushConsumerProxy proxy = supplierAdmin.obtainStructuredPushConsumerProxy();
      proxy.connectStructuredPushSupplier(() -> {});
      return state.bindProxy(proxy);
    }
    if (operation.equals(NotificationServiceDescriptors.OBTAIN_STRUCTURED_PULL_CONSUMER)) {
      LocalStructuredPullConsumerProxy proxy = supplierAdmin.obtainStructuredPullConsumerProxy();
      proxy.connectStructuredPullSupplier(new EmptyPullSupplier());
      return state.bindProxy(proxy);
    }
    throw unsupported(operation);
  }

  private Object invokeConsumerAdmin(
      LocalNotificationConsumerAdmin consumerAdmin, IdlOperationDescriptor operation) {
    if (operation.equals(NotificationServiceDescriptors.OBTAIN_STRUCTURED_PUSH_SUPPLIER)) {
      LocalStructuredPushSupplierProxy proxy = consumerAdmin.obtainStructuredPushSupplierProxy();
      proxy.connectStructuredPushConsumer(new NoopPushConsumer());
      return state.bindProxy(proxy);
    }
    if (operation.equals(NotificationServiceDescriptors.OBTAIN_STRUCTURED_PULL_SUPPLIER)) {
      LocalStructuredPullSupplierProxy proxy = consumerAdmin.obtainStructuredPullSupplierProxy();
      proxy.connectStructuredPullConsumer(() -> {});
      return state.bindProxy(proxy);
    }
    throw unsupported(operation);
  }

  private Object invokePushConsumer(
      LocalStructuredPushConsumerProxy proxy,
      IdlOperationDescriptor operation,
      List<Object> arguments) {
    if (operation.equals(NotificationServiceDescriptors.PUSH_STRUCTURED_EVENT)) {
      proxy.pushStructuredEvent((NotificationStructuredEvent) arguments.get(0));
      return null;
    }
    if (operation.equals(NotificationServiceDescriptors.DISCONNECT_STRUCTURED_PUSH_CONSUMER)) {
      proxy.disconnectStructuredPushSupplier();
      return null;
    }
    throw unsupported(operation);
  }

  private Object invokePullConsumer(
      LocalStructuredPullConsumerProxy proxy, IdlOperationDescriptor operation) {
    if (operation.equals(NotificationServiceDescriptors.DISCONNECT_STRUCTURED_PULL_CONSUMER)) {
      proxy.disconnectStructuredPullSupplier();
      return null;
    }
    throw unsupported(operation);
  }

  private Object invokePushSupplier(
      LocalStructuredPushSupplierProxy proxy,
      IdlOperationDescriptor operation,
      List<Object> arguments) {
    if (operation.equals(NotificationServiceDescriptors.DISCONNECT_STRUCTURED_PUSH_SUPPLIER)) {
      proxy.disconnectStructuredPushConsumer();
      return null;
    }
    if (operation.equals(NotificationServiceDescriptors.SET_FILTER)) {
      proxy.configureFilter(NotificationFilter.parse((String) arguments.get(0)));
      return null;
    }
    if (operation.equals(NotificationServiceDescriptors.SET_INTEGER_QOS)) {
      proxy.configurePolicies(
          NotificationPolicies.from(
              List.of(
                  NotificationPolicyProperty.signedLong(
                      (String) arguments.get(0), ((Long) arguments.get(1)).longValue()))));
      return null;
    }
    if (operation.equals(NotificationServiceDescriptors.SET_BOOLEAN_QOS)) {
      proxy.configurePolicies(
          NotificationPolicies.from(
              List.of(
                  NotificationPolicyProperty.bool(
                      (String) arguments.get(0), ((Boolean) arguments.get(1)).booleanValue()))));
      return null;
    }
    throw unsupported(operation);
  }

  private Object invokePullSupplier(
      LocalStructuredPullSupplierProxy proxy,
      IdlOperationDescriptor operation,
      List<Object> arguments) {
    if (operation.equals(NotificationServiceDescriptors.PULL_STRUCTURED_EVENT)) {
      return proxy.pullStructuredEvent();
    }
    if (operation.equals(NotificationServiceDescriptors.TRY_PULL_STRUCTURED_EVENT)) {
      return proxy
          .tryPullStructuredEvent()
          .map(NotificationTryPullResult::present)
          .orElseGet(NotificationTryPullResult::empty);
    }
    if (operation.equals(NotificationServiceDescriptors.DISCONNECT_STRUCTURED_PULL_SUPPLIER)) {
      proxy.disconnectStructuredPullConsumer();
      return null;
    }
    if (operation.equals(NotificationServiceDescriptors.SET_FILTER)) {
      proxy.configureFilter(NotificationFilter.parse((String) arguments.get(0)));
      return null;
    }
    if (operation.equals(NotificationServiceDescriptors.SET_INTEGER_QOS)) {
      proxy.configurePolicies(
          NotificationPolicies.from(
              List.of(
                  NotificationPolicyProperty.signedLong(
                      (String) arguments.get(0), ((Long) arguments.get(1)).longValue()))));
      return null;
    }
    if (operation.equals(NotificationServiceDescriptors.SET_BOOLEAN_QOS)) {
      proxy.configurePolicies(
          NotificationPolicies.from(
              List.of(
                  NotificationPolicyProperty.bool(
                      (String) arguments.get(0), ((Boolean) arguments.get(1)).booleanValue()))));
      return null;
    }
    throw unsupported(operation);
  }

  private static org.omg.CORBA.BAD_OPERATION unsupported(IdlOperationDescriptor operation) {
    return NotificationServiceCorbaExceptions.badOperation(
        "Unsupported Notification Service operation: " + operation.name());
  }

  private static final class NoopPushConsumer implements NotificationPushConsumer {
    @Override
    public void pushStructuredEvent(NotificationStructuredEvent event) {
      LocalNotificationChannel.requireEvent(event);
    }

    @Override
    public void disconnectStructuredPushConsumer() {}
  }

  private static final class EmptyPullSupplier implements NotificationPullSupplier {
    @Override
    public Optional<NotificationStructuredEvent> pullStructuredEvent() {
      return Optional.empty();
    }

    @Override
    public Optional<NotificationStructuredEvent> tryPullStructuredEvent() {
      return Optional.empty();
    }

    @Override
    public void disconnectStructuredPullSupplier() {}
  }
}
