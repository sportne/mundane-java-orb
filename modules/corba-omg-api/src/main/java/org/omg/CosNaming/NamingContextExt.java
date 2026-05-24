package org.omg.CosNaming;

import org.omg.CosNaming.NamingContextExtPackage.InvalidAddress;
import org.omg.CosNaming.NamingContextPackage.CannotProceed;
import org.omg.CosNaming.NamingContextPackage.InvalidName;
import org.omg.CosNaming.NamingContextPackage.NotFound;

/** Extended CosNaming context compatibility surface. */
public interface NamingContextExt extends NamingContext {

  /** Converts a name to a string. */
  String to_string(NameComponent[] name) throws InvalidName;

  /** Converts a string to a name. */
  NameComponent[] to_name(String name) throws InvalidName;

  /** Converts a URL address and string name to a URL. */
  String to_url(String address, String stringName) throws InvalidAddress, InvalidName;

  /** Resolves a stringified name. */
  org.omg.CORBA.Object resolve_str(String name) throws NotFound, CannotProceed, InvalidName;
}
