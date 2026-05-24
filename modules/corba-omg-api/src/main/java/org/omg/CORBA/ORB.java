package org.omg.CORBA;

import java.util.Properties;
import org.omg.CORBA.ORBPackage.InvalidName;

/** API-only ORB compatibility surface. */
public abstract class ORB {

  /** Creates an ORB compatibility object that reports unsupported behavior. */
  public static ORB init() {
    return new UnsupportedOrb();
  }

  /** Creates an ORB compatibility object that reports unsupported behavior. */
  public static ORB init(String[] args, Properties properties) {
    return new UnsupportedOrb();
  }

  /** Creates an empty Any value. */
  public abstract Any create_any();

  /** Returns a primitive TypeCode for the requested kind. */
  public abstract TypeCode get_primitive_tc(TCKind kind);

  /** Converts an object reference to its string form. */
  public abstract String object_to_string(Object object);

  /** Converts a stringified reference to an object reference. */
  public abstract Object string_to_object(String reference);

  /** Resolves a named initial reference. */
  public abstract Object resolve_initial_references(String identifier) throws InvalidName;

  /** Lists available initial reference identifiers. */
  public abstract String[] list_initial_services();

  /** Runs the ORB event loop. */
  public abstract void run();

  /** Requests ORB shutdown. */
  public abstract void shutdown(boolean waitForCompletion);

  /** Destroys the ORB. */
  public abstract void destroy();

  private static final class UnsupportedOrb extends ORB {

    @Override
    public Any create_any() {
      throw unsupported();
    }

    @Override
    public TypeCode get_primitive_tc(TCKind kind) {
      throw unsupported();
    }

    @Override
    public String object_to_string(Object object) {
      throw unsupported();
    }

    @Override
    public Object string_to_object(String reference) {
      throw unsupported();
    }

    @Override
    public Object resolve_initial_references(String identifier) throws InvalidName {
      throw unsupported();
    }

    @Override
    public String[] list_initial_services() {
      return new String[0];
    }

    @Override
    public void run() {
      throw unsupported();
    }

    @Override
    public void shutdown(boolean waitForCompletion) {
      throw unsupported();
    }

    @Override
    public void destroy() {
      throw unsupported();
    }

    private static NO_IMPLEMENT unsupported() {
      return new NO_IMPLEMENT("ORB runtime behavior is not implemented");
    }
  }
}
