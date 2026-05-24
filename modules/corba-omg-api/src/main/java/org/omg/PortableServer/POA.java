package org.omg.PortableServer;

import org.omg.PortableServer.POAPackage.ObjectAlreadyActive;
import org.omg.PortableServer.POAPackage.ObjectNotActive;
import org.omg.PortableServer.POAPackage.ServantAlreadyActive;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongAdapter;
import org.omg.PortableServer.POAPackage.WrongPolicy;

/** Portable Object Adapter compatibility surface. */
public interface POA extends org.omg.CORBA.Object {

  /** Returns this POA's name. */
  String the_name();

  /** Returns this POA's parent. */
  POA the_parent();

  /** Returns this POA's manager. */
  POAManager the_POAManager();

  /** Activates a servant using an automatically assigned object ID. */
  byte[] activate_object(Servant servant) throws ServantAlreadyActive, WrongPolicy;

  /** Activates a servant using a caller supplied object ID. */
  void activate_object_with_id(byte[] objectId, Servant servant)
      throws ServantAlreadyActive, ObjectAlreadyActive, WrongPolicy;

  /** Deactivates an object ID. */
  void deactivate_object(byte[] objectId) throws ObjectNotActive, WrongPolicy;

  /** Converts a servant to an object reference. */
  org.omg.CORBA.Object servant_to_reference(Servant servant) throws ServantNotActive, WrongPolicy;

  /** Converts an object ID to an object reference. */
  org.omg.CORBA.Object id_to_reference(byte[] objectId) throws ObjectNotActive, WrongPolicy;

  /** Converts a reference to an object ID. */
  byte[] reference_to_id(org.omg.CORBA.Object reference) throws WrongAdapter, WrongPolicy;

  /** Destroys this POA. */
  void destroy(boolean etherealizeObjects, boolean waitForCompletion);
}
