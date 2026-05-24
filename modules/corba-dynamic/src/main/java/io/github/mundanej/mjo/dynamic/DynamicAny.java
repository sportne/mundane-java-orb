package io.github.mundanej.mjo.dynamic;

import io.github.mundanej.mjo.any.AnyAggregateValue;
import io.github.mundanej.mjo.any.AnyValue;
import io.github.mundanej.mjo.typecode.IdlTypeCode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable local DynamicAny value backed by the local Any/TypeCode slice.
 *
 * @param any wrapped Any value
 */
public record DynamicAny(AnyValue<?> any) {

  /** Creates a non-null dynamic value. */
  public DynamicAny {
    Objects.requireNonNull(any, "any");
    DynamicAnyFactory.requireValidAnyPayload(any.typeCode(), any.value());
  }

  /** Returns this value's TypeCode. */
  public IdlTypeCode typeCode() {
    return any.typeCode();
  }

  /** Returns the raw local payload value. */
  public Object value() {
    return any.value();
  }

  /** Returns one aggregate member as a dynamic value. */
  public DynamicAny member(String name) {
    return new DynamicAny(asAggregate().member(name));
  }

  /** Returns a copy with one aggregate member replaced. */
  public DynamicAny withMember(String name, DynamicAny member) {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(member, "member");
    AnyAggregateValue aggregate = asAggregate();
    Map<String, AnyValue<?>> members = new LinkedHashMap<>(aggregate.members());
    if (!members.containsKey(name)) {
      throw new DynamicException(
          DynamicDiagnosticCodes.INVALID_ARGUMENTS, "unknown aggregate member: " + name);
    }
    IdlTypeCode expected =
        typeCode().members().stream()
            .filter(typeMember -> typeMember.name().equals(name))
            .findFirst()
            .orElseThrow()
            .type();
    DynamicAnyFactory.requireType(expected, member.any());
    members.put(name, member.any());
    return new DynamicAny(
        new AnyValue<>(aggregate.typeCode(), new AnyAggregateValue(typeCode(), members)));
  }

  /** Returns a sequence element as a dynamic value. */
  public DynamicAny element(int index) {
    return new DynamicAny(sequenceValues().get(index));
  }

  /** Returns a copy with one sequence element replaced. */
  public DynamicAny withElement(int index, DynamicAny element) {
    Objects.requireNonNull(element, "element");
    List<AnyValue<?>> values = new ArrayList<>(sequenceValues());
    DynamicAnyFactory.requireType(typeCode().elementType().orElseThrow(), element.any());
    values.set(index, element.any());
    return new DynamicAny(new AnyValue<>(typeCode(), List.copyOf(values)));
  }

  @SuppressWarnings("unchecked")
  private List<AnyValue<?>> sequenceValues() {
    if (!(any.value() instanceof List<?> values)) {
      throw new DynamicException(
          DynamicDiagnosticCodes.TYPE_MISMATCH, "dynamic value is not a sequence");
    }
    return (List<AnyValue<?>>) values;
  }

  private AnyAggregateValue asAggregate() {
    if (!(any.value() instanceof AnyAggregateValue aggregate)) {
      throw new DynamicException(
          DynamicDiagnosticCodes.TYPE_MISMATCH, "dynamic value is not an aggregate");
    }
    return aggregate;
  }
}
