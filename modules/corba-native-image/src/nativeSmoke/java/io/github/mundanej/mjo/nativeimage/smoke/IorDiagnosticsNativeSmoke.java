package io.github.mundanej.mjo.nativeimage.smoke;

import io.github.mundanej.mjo.ior.CorbalocAddress;
import io.github.mundanej.mjo.ior.CorbalocUrl;
import io.github.mundanej.mjo.ior.CorbanameUrl;
import io.github.mundanej.mjo.ior.IiopProfile;
import io.github.mundanej.mjo.ior.IiopVersion;
import io.github.mundanej.mjo.ior.Ior;
import io.github.mundanej.mjo.ior.ObjectKey;
import io.github.mundanej.mjo.ior.StringifiedIor;
import io.github.mundanej.mjo.ior.TaggedProfile;
import io.github.mundanej.mjo.orb.DurableObjectKey;
import io.github.mundanej.mjo.orb.OrbIdentity;
import java.util.List;

/** Native Image smoke entry point for IOR and object URL diagnostics. */
public final class IorDiagnosticsNativeSmoke {

  private IorDiagnosticsNativeSmoke() {}

  /** Parses and formats representative IOR, corbaloc, and corbaname values. */
  public static void main(String[] args) {
    IiopProfile profile =
        new IiopProfile(
            IiopVersion.V1_2, "127.0.0.1", 2809, new ObjectKey(new byte[] {1, 2, 3}), List.of());
    Ior ior = new Ior("IDL:nativeimage/Greeter:1.0", List.of(TaggedProfile.internetIop(profile)));
    Ior parsed = StringifiedIor.parse(StringifiedIor.format(ior));
    IiopProfile parsedProfile = parsed.profiles().getFirst().internetIopProfile().orElseThrow();

    SmokeAssertions.requireEquals(ior.typeId(), parsed.typeId(), "type id");
    SmokeAssertions.requireEquals("127.0.0.1", parsedProfile.host(), "profile host");
    SmokeAssertions.requireEquals(2809, parsedProfile.port(), "profile port");

    CorbalocUrl corbaloc = CorbalocUrl.parse("corbaloc:iiop:1.2@localhost:2809/Native");
    SmokeAssertions.requireEquals(
        CorbalocAddress.Kind.IIOP, corbaloc.addresses().getFirst().kind(), "corbaloc kind");
    SmokeAssertions.requireEquals("Native", corbaloc.keyString(), "corbaloc key");

    CorbanameUrl corbaname = CorbanameUrl.parse("corbaname:rir:#apps/service");
    SmokeAssertions.requireEquals("apps/service", corbaname.stringName(), "corbaname name");

    OrbIdentity identity = OrbIdentity.durable("native-orb");
    DurableObjectKey key =
        DurableObjectKey.fromPoaPath(
            identity.requireDurableOrbId(), "/RootPOA/apps", new byte[] {4, 5, 6}, 1);
    DurableObjectKey decoded = DurableObjectKey.decode(key.encode());
    SmokeAssertions.requireEquals(identity.requireDurableOrbId(), decoded.orbId(), "orb id");
    SmokeAssertions.requireEquals("/RootPOA/apps", decoded.poaPathString(), "POA path");
  }
}
