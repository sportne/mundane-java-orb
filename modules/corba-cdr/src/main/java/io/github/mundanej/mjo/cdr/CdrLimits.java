package io.github.mundanej.mjo.cdr;

import io.github.mundanej.mjo.common.BoundedLimit;
import java.util.Objects;

/**
 * Named bounds for length-bearing CDR values.
 *
 * @param stringOctets maximum encoded narrow string octets, including the terminating null
 * @param sequenceElements maximum decoded sequence or fixed-array element count
 * @param encapsulationOctets maximum encoded encapsulation payload octets
 */
public record CdrLimits(
    BoundedLimit stringOctets, BoundedLimit sequenceElements, BoundedLimit encapsulationOctets) {

  private static final long DEFAULT_STRING_OCTETS = 65_536L;
  private static final long DEFAULT_SEQUENCE_ELEMENTS = 65_536L;
  private static final long DEFAULT_ENCAPSULATION_OCTETS = 1_048_576L;

  /** Creates validated CDR limits. */
  public CdrLimits {
    Objects.requireNonNull(stringOctets, "stringOctets");
    Objects.requireNonNull(sequenceElements, "sequenceElements");
    Objects.requireNonNull(encapsulationOctets, "encapsulationOctets");
  }

  /** Returns conservative default limits for the first bounded collection slice. */
  public static CdrLimits defaults() {
    return new CdrLimits(
        new BoundedLimit("cdr-string-octets", DEFAULT_STRING_OCTETS),
        new BoundedLimit("cdr-sequence-elements", DEFAULT_SEQUENCE_ELEMENTS),
        new BoundedLimit("cdr-encapsulation-octets", DEFAULT_ENCAPSULATION_OCTETS));
  }
}
