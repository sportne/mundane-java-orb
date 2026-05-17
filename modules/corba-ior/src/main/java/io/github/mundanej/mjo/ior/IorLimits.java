package io.github.mundanej.mjo.ior;

import io.github.mundanej.mjo.cdr.CdrLimits;
import io.github.mundanej.mjo.common.BoundedLimit;
import io.github.mundanej.mjo.common.LimitViolation;
import java.util.Objects;

/**
 * Bounds for IOR, IIOP profile, and object URL parsing.
 *
 * @param stringOctets maximum CDR string octets, including the terminating null
 * @param sequenceElements maximum generic CDR sequence element count
 * @param encapsulationOctets maximum encoded CDR encapsulation octets
 * @param profileCount maximum profiles in one IOR
 * @param profileDataOctets maximum profile data octets
 * @param componentCount maximum tagged components in one profile
 * @param componentDataOctets maximum tagged component data octets
 * @param objectKeyOctets maximum object-key octets
 * @param objectUrlCharacters maximum corbaloc/corbaname characters
 */
public record IorLimits(
    BoundedLimit stringOctets,
    BoundedLimit sequenceElements,
    BoundedLimit encapsulationOctets,
    BoundedLimit profileCount,
    BoundedLimit profileDataOctets,
    BoundedLimit componentCount,
    BoundedLimit componentDataOctets,
    BoundedLimit objectKeyOctets,
    BoundedLimit objectUrlCharacters) {

  private static final long DEFAULT_STRING_OCTETS = 65_536L;
  private static final long DEFAULT_SEQUENCE_ELEMENTS = 65_536L;
  private static final long DEFAULT_ENCAPSULATION_OCTETS = 1_048_576L;
  private static final long DEFAULT_PROFILE_COUNT = 128L;
  private static final long DEFAULT_PROFILE_DATA_OCTETS = 1_048_576L;
  private static final long DEFAULT_COMPONENT_COUNT = 512L;
  private static final long DEFAULT_COMPONENT_DATA_OCTETS = 1_048_576L;
  private static final long DEFAULT_OBJECT_KEY_OCTETS = 65_536L;
  private static final long DEFAULT_OBJECT_URL_CHARACTERS = 65_536L;

  /** Creates validated IOR limits. */
  public IorLimits {
    Objects.requireNonNull(stringOctets, "stringOctets");
    Objects.requireNonNull(sequenceElements, "sequenceElements");
    Objects.requireNonNull(encapsulationOctets, "encapsulationOctets");
    Objects.requireNonNull(profileCount, "profileCount");
    Objects.requireNonNull(profileDataOctets, "profileDataOctets");
    Objects.requireNonNull(componentCount, "componentCount");
    Objects.requireNonNull(componentDataOctets, "componentDataOctets");
    Objects.requireNonNull(objectKeyOctets, "objectKeyOctets");
    Objects.requireNonNull(objectUrlCharacters, "objectUrlCharacters");
  }

  /** Returns conservative default limits for IOR and object URL parsing. */
  public static IorLimits defaults() {
    return new IorLimits(
        new BoundedLimit("ior-string-octets", DEFAULT_STRING_OCTETS),
        new BoundedLimit("ior-sequence-elements", DEFAULT_SEQUENCE_ELEMENTS),
        new BoundedLimit("ior-encapsulation-octets", DEFAULT_ENCAPSULATION_OCTETS),
        new BoundedLimit("ior-profile-count", DEFAULT_PROFILE_COUNT),
        new BoundedLimit("ior-profile-data-octets", DEFAULT_PROFILE_DATA_OCTETS),
        new BoundedLimit("ior-component-count", DEFAULT_COMPONENT_COUNT),
        new BoundedLimit("ior-component-data-octets", DEFAULT_COMPONENT_DATA_OCTETS),
        new BoundedLimit("ior-object-key-octets", DEFAULT_OBJECT_KEY_OCTETS),
        new BoundedLimit("ior-object-url-characters", DEFAULT_OBJECT_URL_CHARACTERS));
  }

  /** Returns equivalent CDR limits for nested CDR readers and writers. */
  public CdrLimits cdrLimits() {
    return new CdrLimits(stringOctets, sequenceElements, encapsulationOctets);
  }

  void requireWithin(BoundedLimit limit, long observedValue) {
    limit.check(observedValue).ifPresent(IorLimits::throwLimitExceeded);
  }

  private static void throwLimitExceeded(LimitViolation violation) {
    throw new IorException(IorDiagnosticCodes.LENGTH_LIMIT_EXCEEDED, violation.message());
  }
}
