package io.github.mundanej.mjo.any;

import io.github.mundanej.mjo.typecode.IdlTypeCode;
import java.util.Map;
import java.util.Objects;

/**
 * Explicit value shape for struct and exception Any payloads.
 *
 * @param typeCode struct or exception TypeCode
 * @param members member values keyed by IDL member name
 */
public record AnyAggregateValue(IdlTypeCode typeCode, Map<String, AnyValue<?>> members) {

  /** Creates an immutable aggregate value. */
  public AnyAggregateValue {
    Objects.requireNonNull(typeCode, "typeCode");
    if (!typeCode.isAggregate()) {
      throw new AnyException(
          AnyDiagnosticCodes.TYPE_MISMATCH,
          "aggregate value requires a struct or exception TypeCode: " + typeCode.kind());
    }
    members = Map.copyOf(Objects.requireNonNull(members, "members"));
  }

  /** Returns the member value for the supplied IDL name. */
  public AnyValue<?> member(String name) {
    Objects.requireNonNull(name, "name");
    AnyValue<?> value = members.get(name);
    if (value == null) {
      throw new AnyException(
          AnyDiagnosticCodes.MISSING_MEMBER, "missing aggregate member: " + name);
    }
    return value;
  }
}
