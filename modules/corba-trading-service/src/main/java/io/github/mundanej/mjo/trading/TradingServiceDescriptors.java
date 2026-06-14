package io.github.mundanej.mjo.trading;

import io.github.mundanej.mjo.iiop.IiopOperationBinding;
import io.github.mundanej.mjo.repositoryid.RepositoryId;
import io.github.mundanej.mjo.typecode.IdlGeneratedTypeDescriptor;
import io.github.mundanej.mjo.typecode.IdlOperationDescriptor;
import io.github.mundanej.mjo.typecode.IdlParameterDescriptor;
import io.github.mundanej.mjo.typecode.IdlParameterMode;
import io.github.mundanej.mjo.typecode.IdlTypeKind;
import io.github.mundanej.mjo.typecode.IdlTypeReference;
import java.util.List;
import java.util.Optional;

/** Static descriptors and IIOP bindings for the supported Trading Service subset. */
public final class TradingServiceDescriptors {

  /** Repository ID for the supported CosTrading Lookup/Register local trader facade. */
  public static final RepositoryId TRADER_REPOSITORY_ID =
      RepositoryId.parse("IDL:omg.org/CosTrading/Lookup:1.0");

  /** IDL void return type. */
  public static final IdlTypeReference VOID_TYPE =
      new IdlTypeReference(IdlTypeKind.VOID, "void", "void", Optional.empty());

  /** IDL string type. */
  public static final IdlTypeReference STRING_TYPE = primitive("string", String.class.getName());

  /** IDL boolean type. */
  public static final IdlTypeReference BOOLEAN_TYPE = primitive("boolean", "boolean");

  /** IDL signed long long type. */
  public static final IdlTypeReference LONG_LONG_TYPE = primitive("long long", "long");

  /** IDL double type. */
  public static final IdlTypeReference DOUBLE_TYPE = primitive("double", "double");

  /** Supported Trading Service type metadata struct. */
  public static final IdlTypeReference SERVICE_TYPE_TYPE =
      tradingStruct("::CosTrading::ServiceType", TradingServiceType.class.getName());

  /** Supported Trading Service offer metadata struct. */
  public static final IdlTypeReference OFFER_TYPE =
      tradingStruct("::CosTrading::Offer", TradingOffer.class.getName());

  /** Supported Trading Service import/export link metadata struct. */
  public static final IdlTypeReference LINK_TYPE =
      tradingStruct("::CosTrading::ImportExportLink", TradingImportExportLink.class.getName());

  /** Supported Trading Service trader object-reference type. */
  public static final IdlTypeReference TRADER_TYPE =
      new IdlTypeReference(
          IdlTypeKind.INTERFACE,
          "::CosTrading::Lookup",
          NetworkTradingServiceState.class.getName(),
          Optional.of(TRADER_REPOSITORY_ID));

  /** Trader::register_type. */
  public static final IdlOperationDescriptor REGISTER_TYPE =
      operation("register_type", VOID_TYPE, parameter("type", SERVICE_TYPE_TYPE));

  /** Trader::update_type. */
  public static final IdlOperationDescriptor UPDATE_TYPE =
      operation("update_type", VOID_TYPE, parameter("type", SERVICE_TYPE_TYPE));

  /** Trader::delete_type. */
  public static final IdlOperationDescriptor DELETE_TYPE =
      operation("delete_type", SERVICE_TYPE_TYPE, parameter("name", STRING_TYPE));

  /** Trader::lookup_type. */
  public static final IdlOperationDescriptor LOOKUP_TYPE =
      operation("lookup_type", SERVICE_TYPE_TYPE, parameter("name", STRING_TYPE));

  /** Trader::list_types. */
  public static final IdlOperationDescriptor LIST_TYPES =
      new IdlOperationDescriptor("list_types", SERVICE_TYPE_TYPE, List.of(), List.of());

  /** Trader::register_offer. */
  public static final IdlOperationDescriptor REGISTER_OFFER =
      operation("register_offer", VOID_TYPE, parameter("offer", OFFER_TYPE));

  /** Trader::withdraw_offer. */
  public static final IdlOperationDescriptor WITHDRAW_OFFER =
      operation("withdraw_offer", OFFER_TYPE, parameter("id", STRING_TYPE));

  /** Trader::query_offers. */
  public static final IdlOperationDescriptor QUERY_OFFERS =
      new IdlOperationDescriptor(
          "query_offers",
          OFFER_TYPE,
          List.of(
              parameter("type_name", STRING_TYPE), parameter("constraint_expression", STRING_TYPE)),
          List.of());

  /** Trader::register_import_link. */
  public static final IdlOperationDescriptor REGISTER_IMPORT_LINK =
      new IdlOperationDescriptor(
          "register_import_link",
          VOID_TYPE,
          List.of(parameter("name", STRING_TYPE), parameter("peer_trader_name", STRING_TYPE)),
          List.of());

  /** Trader::register_export_link. */
  public static final IdlOperationDescriptor REGISTER_EXPORT_LINK =
      new IdlOperationDescriptor(
          "register_export_link",
          VOID_TYPE,
          List.of(parameter("name", STRING_TYPE), parameter("peer_trader_name", STRING_TYPE)),
          List.of());

  /** Trader::list_import_export_links. */
  public static final IdlOperationDescriptor LIST_IMPORT_EXPORT_LINKS =
      new IdlOperationDescriptor("list_import_export_links", LINK_TYPE, List.of(), List.of());

  /** Trader::reject_remote_import_query. */
  public static final IdlOperationDescriptor REJECT_REMOTE_IMPORT_QUERY =
      operation("reject_remote_import_query", VOID_TYPE, parameter("link_name", STRING_TYPE));

  /** Descriptor for the supported CosTrading trader facade. */
  public static final IdlGeneratedTypeDescriptor TRADER =
      new IdlGeneratedTypeDescriptor(
          IdlTypeKind.INTERFACE,
          "::CosTrading::Lookup",
          NetworkTradingServiceState.class.getName(),
          TRADER_REPOSITORY_ID,
          List.of(),
          List.of(),
          List.of(
              REGISTER_TYPE,
              UPDATE_TYPE,
              DELETE_TYPE,
              LOOKUP_TYPE,
              LIST_TYPES,
              REGISTER_OFFER,
              WITHDRAW_OFFER,
              QUERY_OFFERS,
              REGISTER_IMPORT_LINK,
              REGISTER_EXPORT_LINK,
              LIST_IMPORT_EXPORT_LINKS,
              REJECT_REMOTE_IMPORT_QUERY));

  private static final List<IiopOperationBinding> IIOP_OPERATION_BINDINGS =
      List.of(
          binding(REGISTER_TYPE),
          binding(UPDATE_TYPE),
          binding(DELETE_TYPE),
          binding(LOOKUP_TYPE),
          binding(LIST_TYPES),
          binding(REGISTER_OFFER),
          binding(WITHDRAW_OFFER),
          binding(QUERY_OFFERS),
          binding(REGISTER_IMPORT_LINK),
          binding(REGISTER_EXPORT_LINK),
          binding(LIST_IMPORT_EXPORT_LINKS),
          binding(REJECT_REMOTE_IMPORT_QUERY));

  private TradingServiceDescriptors() {}

  /** Returns immutable IIOP operation bindings for the supported Trading Service subset. */
  public static List<IiopOperationBinding> iiopOperationBindings() {
    return IIOP_OPERATION_BINDINGS;
  }

  private static IiopOperationBinding binding(IdlOperationDescriptor operation) {
    return new IiopOperationBinding(operation, TradingServiceIiopCodec.INSTANCE);
  }

  private static IdlOperationDescriptor operation(
      String name, IdlTypeReference returnType, IdlParameterDescriptor parameter) {
    return new IdlOperationDescriptor(name, returnType, List.of(parameter), List.of());
  }

  private static IdlParameterDescriptor parameter(String name, IdlTypeReference type) {
    return new IdlParameterDescriptor(name, IdlParameterMode.IN, type);
  }

  private static IdlTypeReference primitive(String idlName, String javaName) {
    return new IdlTypeReference(IdlTypeKind.PRIMITIVE, idlName, javaName, Optional.empty());
  }

  private static IdlTypeReference tradingStruct(String idlName, String javaName) {
    return new IdlTypeReference(IdlTypeKind.STRUCT, idlName, javaName, Optional.empty());
  }
}
