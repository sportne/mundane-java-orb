package io.github.mundanej.mjo.time;

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

/** Static descriptors and IIOP operation bindings for the supported Time Service subset. */
public final class TimeServiceDescriptors {

  /** Repository ID for the TimeBase UtcT value shape. */
  public static final RepositoryId UTC_TIME_REPOSITORY_ID =
      RepositoryId.parse("IDL:omg.org/TimeBase/UtcT:1.0");

  /** Repository ID for the TimeBase IntervalT value shape. */
  public static final RepositoryId INTERVAL_REPOSITORY_ID =
      RepositoryId.parse("IDL:omg.org/TimeBase/IntervalT:1.0");

  /** Repository ID for the CosTime TimeService interface. */
  public static final RepositoryId TIME_SERVICE_REPOSITORY_ID =
      RepositoryId.parse("IDL:omg.org/CosTime/TimeService:1.0");

  /** TimeBase UtcT type reference used by operation descriptors. */
  public static final IdlTypeReference UTC_TIME_TYPE =
      new IdlTypeReference(
          IdlTypeKind.STRUCT,
          "::TimeBase::UtcT",
          UtcTime.class.getName(),
          Optional.of(UTC_TIME_REPOSITORY_ID));

  /** TimeBase IntervalT type reference used by operation descriptors. */
  public static final IdlTypeReference INTERVAL_TYPE =
      new IdlTypeReference(
          IdlTypeKind.STRUCT,
          "::TimeBase::IntervalT",
          TimeInterval.class.getName(),
          Optional.of(INTERVAL_REPOSITORY_ID));

  /** IDL unsigned long long represented by the supported Java nonnegative long subset. */
  public static final IdlTypeReference UNSIGNED_LONG_LONG_TYPE =
      primitive("unsigned long long", "long");

  /** IDL unsigned long represented as a Java long to preserve unsigned 32-bit values. */
  public static final IdlTypeReference UNSIGNED_LONG_TYPE = primitive("unsigned long", "long");

  /** IDL unsigned short represented as a Java int to preserve unsigned 16-bit values. */
  public static final IdlTypeReference UNSIGNED_SHORT_TYPE = primitive("unsigned short", "int");

  /** IDL short represented as Java short. */
  public static final IdlTypeReference SHORT_TYPE = primitive("short", "short");

  /** Descriptor for TimeService::universal_time. */
  public static final IdlOperationDescriptor UNIVERSAL_TIME =
      new IdlOperationDescriptor("universal_time", UTC_TIME_TYPE, List.of(), List.of());

  /** Descriptor for TimeService::new_universal_time. */
  public static final IdlOperationDescriptor NEW_UNIVERSAL_TIME =
      new IdlOperationDescriptor(
          "new_universal_time",
          UTC_TIME_TYPE,
          List.of(
              new IdlParameterDescriptor("time", IdlParameterMode.IN, UNSIGNED_LONG_LONG_TYPE),
              new IdlParameterDescriptor("inacclo", IdlParameterMode.IN, UNSIGNED_LONG_TYPE),
              new IdlParameterDescriptor("inacchi", IdlParameterMode.IN, UNSIGNED_SHORT_TYPE),
              new IdlParameterDescriptor("tdf", IdlParameterMode.IN, SHORT_TYPE)),
          List.of());

  /** Descriptor for TimeService::new_interval. */
  public static final IdlOperationDescriptor NEW_INTERVAL =
      new IdlOperationDescriptor(
          "new_interval",
          INTERVAL_TYPE,
          List.of(
              new IdlParameterDescriptor(
                  "lower_bound", IdlParameterMode.IN, UNSIGNED_LONG_LONG_TYPE),
              new IdlParameterDescriptor(
                  "upper_bound", IdlParameterMode.IN, UNSIGNED_LONG_LONG_TYPE)),
          List.of());

  /** Descriptor for the supported CosTime TimeService subset. */
  public static final IdlGeneratedTypeDescriptor TIME_SERVICE =
      new IdlGeneratedTypeDescriptor(
          IdlTypeKind.INTERFACE,
          "::CosTime::TimeService",
          LocalTimeService.class.getName(),
          TIME_SERVICE_REPOSITORY_ID,
          List.of(),
          List.of(),
          List.of(UNIVERSAL_TIME, NEW_UNIVERSAL_TIME, NEW_INTERVAL));

  private static final List<IiopOperationBinding> IIOP_OPERATION_BINDINGS =
      List.of(
          new IiopOperationBinding(UNIVERSAL_TIME, TimeServiceIiopCodec.INSTANCE),
          new IiopOperationBinding(NEW_UNIVERSAL_TIME, TimeServiceIiopCodec.INSTANCE),
          new IiopOperationBinding(NEW_INTERVAL, TimeServiceIiopCodec.INSTANCE));

  private TimeServiceDescriptors() {}

  /** Returns the immutable IIOP operation bindings for this Time Service subset. */
  public static List<IiopOperationBinding> iiopOperationBindings() {
    return IIOP_OPERATION_BINDINGS;
  }

  static long inaccuracyTicks(long inacclo, int inacchi) {
    requireUnsignedLong(inacclo, "inacclo");
    requireUnsignedShort(inacchi, "inacchi");
    return (((long) inacchi) << 32) | inacclo;
  }

  static long inacclo(UtcTime time) {
    return time.inaccuracyTicks() & 0xFFFF_FFFFL;
  }

  static int inacchi(UtcTime time) {
    return Math.toIntExact(time.inaccuracyTicks() >>> 32);
  }

  private static IdlTypeReference primitive(String idlName, String javaName) {
    return new IdlTypeReference(IdlTypeKind.PRIMITIVE, idlName, javaName, Optional.empty());
  }

  private static void requireUnsignedLong(long value, String name) {
    if (value < 0L || value > 0xFFFF_FFFFL) {
      throw TimeServiceCorbaExceptions.badParam(name + " must fit in unsigned long");
    }
  }

  private static void requireUnsignedShort(int value, String name) {
    if (value < 0 || value > 0xFFFF) {
      throw TimeServiceCorbaExceptions.badParam(name + " must fit in unsigned short");
    }
  }
}
