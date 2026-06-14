package io.github.mundanej.mjo.trading;

import io.github.mundanej.mjo.modern.LocalInvocationDispatcher;
import io.github.mundanej.mjo.modern.LocalInvocationRequest;
import io.github.mundanej.mjo.typecode.IdlOperationDescriptor;
import java.util.List;
import java.util.Objects;

final class TradingServiceDispatcher implements LocalInvocationDispatcher {

  private final NetworkTradingServiceState state;

  TradingServiceDispatcher(NetworkTradingServiceState state) {
    this.state = Objects.requireNonNull(state, "state");
  }

  @Override
  public Object invoke(LocalInvocationRequest request) {
    try {
      IdlOperationDescriptor operation = request.operation();
      List<Object> arguments = request.arguments();
      if (operation.equals(TradingServiceDescriptors.REGISTER_TYPE)) {
        state.registerType((TradingServiceType) arguments.get(0));
        return null;
      }
      if (operation.equals(TradingServiceDescriptors.UPDATE_TYPE)) {
        state.updateType((TradingServiceType) arguments.get(0));
        return null;
      }
      if (operation.equals(TradingServiceDescriptors.DELETE_TYPE)) {
        return state.deleteType((String) arguments.get(0));
      }
      if (operation.equals(TradingServiceDescriptors.LOOKUP_TYPE)) {
        return state.lookupType((String) arguments.get(0));
      }
      if (operation.equals(TradingServiceDescriptors.LIST_TYPES)) {
        return state.listTypes();
      }
      if (operation.equals(TradingServiceDescriptors.REGISTER_OFFER)) {
        state.registerOffer((TradingOffer) arguments.get(0));
        return null;
      }
      if (operation.equals(TradingServiceDescriptors.WITHDRAW_OFFER)) {
        return state.withdrawOffer((String) arguments.get(0));
      }
      if (operation.equals(TradingServiceDescriptors.QUERY_OFFERS)) {
        return state.queryOffers((String) arguments.get(0), (String) arguments.get(1));
      }
      if (operation.equals(TradingServiceDescriptors.REGISTER_IMPORT_LINK)) {
        state.registerImportLink((String) arguments.get(0), (String) arguments.get(1));
        return null;
      }
      if (operation.equals(TradingServiceDescriptors.REGISTER_EXPORT_LINK)) {
        state.registerExportLink((String) arguments.get(0), (String) arguments.get(1));
        return null;
      }
      if (operation.equals(TradingServiceDescriptors.LIST_IMPORT_EXPORT_LINKS)) {
        return state.listImportExportLinks();
      }
      if (operation.equals(TradingServiceDescriptors.REJECT_REMOTE_IMPORT_QUERY)) {
        state.rejectRemoteImportQuery((String) arguments.get(0));
        return null;
      }
      throw TradingServiceCorbaExceptions.badOperation(
          "Unsupported Trading Service operation: " + operation.name());
    } catch (TradingServiceException exception) {
      throw TradingServiceCorbaExceptions.from(exception);
    }
  }
}
