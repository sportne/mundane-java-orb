package org.omg.DynamicAny;

import org.omg.DynamicAny.DynAnyFactoryPackage.InconsistentTypeCode;

/** Factory for DynamicAny compatibility values. */
public interface DynAnyFactory {

  /** Creates a DynamicAny from an Any. */
  DynAny create_dyn_any(org.omg.CORBA.Any value) throws InconsistentTypeCode;

  /** Creates a DynamicAny from a TypeCode. */
  DynAny create_dyn_any_from_type_code(org.omg.CORBA.TypeCode type) throws InconsistentTypeCode;
}
