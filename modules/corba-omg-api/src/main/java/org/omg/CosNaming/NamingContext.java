package org.omg.CosNaming;

import org.omg.CosNaming.NamingContextPackage.AlreadyBound;
import org.omg.CosNaming.NamingContextPackage.CannotProceed;
import org.omg.CosNaming.NamingContextPackage.InvalidName;
import org.omg.CosNaming.NamingContextPackage.NotEmpty;
import org.omg.CosNaming.NamingContextPackage.NotFound;

/** CosNaming context compatibility surface. */
public interface NamingContext extends org.omg.CORBA.Object {

  /** Binds a name to an object. */
  void bind(NameComponent[] name, org.omg.CORBA.Object object)
      throws NotFound, CannotProceed, InvalidName, AlreadyBound;

  /** Rebinds a name to an object. */
  void rebind(NameComponent[] name, org.omg.CORBA.Object object)
      throws NotFound, CannotProceed, InvalidName;

  /** Resolves a name. */
  org.omg.CORBA.Object resolve(NameComponent[] name) throws NotFound, CannotProceed, InvalidName;

  /** Unbinds a name. */
  void unbind(NameComponent[] name) throws NotFound, CannotProceed, InvalidName;

  /** Lists bindings. */
  void list(int howMany, BindingListHolder bindings, BindingIteratorHolder iterator);

  /** Creates a child naming context. */
  NamingContext new_context();

  /** Binds a new child naming context. */
  NamingContext bind_new_context(NameComponent[] name)
      throws NotFound, AlreadyBound, CannotProceed, InvalidName;

  /** Destroys this context. */
  void destroy() throws NotEmpty;
}
