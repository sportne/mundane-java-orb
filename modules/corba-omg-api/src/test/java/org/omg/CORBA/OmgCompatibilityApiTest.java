package org.omg.CORBA;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.omg.CORBA.portable.ApplicationException;
import org.omg.CORBA.portable.Delegate;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.ObjectImpl;
import org.omg.CORBA.portable.OutputStream;
import org.omg.CORBA.portable.RemarshalException;
import org.omg.CORBA.portable.ServantObject;
import org.omg.CosNaming.Binding;
import org.omg.CosNaming.BindingHolder;
import org.omg.CosNaming.BindingIteratorHolder;
import org.omg.CosNaming.BindingListHolder;
import org.omg.CosNaming.BindingType;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.CosNaming.NamingContextExtHolder;
import org.omg.CosNaming.NamingContextHolder;
import org.omg.CosNaming.NamingContextPackage.CannotProceed;
import org.omg.CosNaming.NamingContextPackage.NotFound;
import org.omg.CosNaming.NamingContextPackage.NotFoundReason;
import org.omg.DynamicAny.DynAnyHolder;
import org.omg.DynamicAny.NameDynAnyPair;
import org.omg.DynamicAny.NameValuePair;
import org.omg.PortableInterceptor.ForwardRequest;
import org.omg.PortableInterceptor.ORBInitInfoPackage.DuplicateName;
import org.omg.PortableServer.IdAssignmentPolicyValue;
import org.omg.PortableServer.LifespanPolicyValue;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.POAHolder;
import org.omg.PortableServer.POAManagerHolder;
import org.omg.PortableServer.POAPackage.InvalidPolicy;
import org.omg.PortableServer.Servant;
import org.omg.PortableServer.State;
import org.omg.PortableServer.ThreadPolicyValue;

/** API contract tests for the G10 OMG compatibility surface. */
@Tag("unit")
final class OmgCompatibilityApiTest {

  @TempDir private Path tempDir;

  @Test
  void generatedStyleFixtureSourcesCompileAgainstCompatibilityApi() throws Exception {
    compile(
        "fixture/GeneratedCompatibilityFixture.java",
        """
        package fixture;

        final class Problem extends org.omg.CORBA.UserException {
          public String reason;
          Problem() {}
          Problem(String reason) { this.reason = reason; }
        }

        interface Echo extends org.omg.CORBA.Object {
          String say(String value) throws Problem;
        }

        final class EchoHolder implements org.omg.CORBA.portable.Streamable {
          public Echo value;
          EchoHolder() {}
          EchoHolder(Echo value) { this.value = value; }
          public void _read(org.omg.CORBA.portable.InputStream input) {
            value = EchoHelper.read(input);
          }
          public void _write(org.omg.CORBA.portable.OutputStream output) {
            EchoHelper.write(output, value);
          }
          public org.omg.CORBA.TypeCode _type() {
            return EchoHelper.type();
          }
        }

        final class EchoHelper {
          static String id() { return "IDL:fixture/Echo:1.0"; }
          static org.omg.CORBA.TypeCode type() { return null; }
          static Echo narrow(org.omg.CORBA.Object object) { return (Echo) object; }
          static Echo read(org.omg.CORBA.portable.InputStream input) {
            return narrow(input.read_Object());
          }
          static void write(org.omg.CORBA.portable.OutputStream output, Echo value) {
            output.write_Object(value);
          }
        }

        abstract class EchoPOA extends org.omg.PortableServer.Servant implements Echo {
          public String[] _all_interfaces(org.omg.PortableServer.POA poa, byte[] objectId) {
            return new String[] { EchoHelper.id() };
          }
          public org.omg.CORBA.portable.OutputStream _invoke(
              String operation,
              org.omg.CORBA.portable.InputStream input,
              org.omg.CORBA.portable.ResponseHandler handler) {
            return handler.createReply();
          }
        }

        abstract class _EchoStub extends org.omg.CORBA.portable.ObjectImpl implements Echo {
          public String[] _ids() { return new String[] { EchoHelper.id() }; }
          public String say(String value) throws Problem {
            org.omg.CORBA.portable.OutputStream output = _request("say", true);
            output.write_string(value);
            try {
              return _invoke(output).read_string();
            } catch (org.omg.CORBA.portable.ApplicationException exception) {
              throw new Problem(exception.getId());
            } catch (org.omg.CORBA.portable.RemarshalException exception) {
              return say(value);
            }
          }
        }

        final class NamingAndDynamicUse {
          void use(
              org.omg.CosNaming.NamingContextExt naming,
              org.omg.DynamicAny.DynAnyFactory factory,
              org.omg.PortableInterceptor.ORBInitInfo info)
              throws Exception {
            org.omg.CosNaming.NameComponent[] name = naming.to_name("a.b");
            org.omg.CORBA.Object object = naming.resolve_str("a.b");
            factory.create_dyn_any_from_type_code(null);
            info.add_client_request_interceptor(null);
            if (name.length == 0 || object == null) {
              throw new org.omg.CORBA.NO_IMPLEMENT();
            }
          }
        }
        """);
  }

  @Test
  void enumLikeValuesAndStructsExposeStableFields() {
    int[] typeKinds = {
      TCKind._tk_null,
      TCKind._tk_void,
      TCKind._tk_short,
      TCKind._tk_long,
      TCKind._tk_string,
      TCKind._tk_objref,
      TCKind._tk_struct,
      TCKind._tk_union,
      TCKind._tk_enum,
      TCKind._tk_sequence,
      TCKind._tk_array,
      TCKind._tk_alias,
      TCKind._tk_except
    };
    for (int typeKind : typeKinds) {
      assertEquals(typeKind, TCKind.from_int(typeKind).value());
    }
    assertEquals(TCKind._tk_abstract_interface, TCKind._tk_abstract_interface);
    assertEquals(BindingType._ncontext, BindingType.from_int(BindingType._ncontext).value());
    assertEquals(BindingType._nobject, BindingType.from_int(BindingType._nobject).value());
    assertEquals(SetOverrideType._ADD_OVERRIDE, SetOverrideType.from_int(1).value());
    assertEquals(SetOverrideType._SET_OVERRIDE, SetOverrideType.from_int(0).value());
    assertThrows(BAD_PARAM.class, () -> TCKind.from_int(-1));
    assertThrows(BAD_PARAM.class, () -> BindingType.from_int(99));
    assertThrows(BAD_PARAM.class, () -> SetOverrideType.from_int(99));

    StructMember structMember = new StructMember("field", null, null);
    UnionMember unionMember = new UnionMember("choice", null, null, null);
    NameComponent name = new NameComponent("alpha", "kind");
    Binding binding = new Binding(new NameComponent[] {name}, BindingType.nobject);
    NameValuePair anyPair = new NameValuePair("value", null);
    NameDynAnyPair dynPair = new NameDynAnyPair("dyn", null);
    ValueMember valueMember = new ValueMember();
    valueMember.name = "member";
    valueMember.id = "IDL:member:1.0";
    valueMember.defined_in = "IDL:container:1.0";
    valueMember.version = "1.0";
    valueMember.access = 1;
    ServantObject servantObject = new ServantObject();
    servantObject.servant = "servant";

    assertEquals("field", structMember.name);
    assertNull(structMember.type);
    assertNull(structMember.type_def);
    assertEquals("choice", unionMember.name);
    assertNull(unionMember.label);
    assertNull(unionMember.type);
    assertNull(unionMember.type_def);
    assertEquals("alpha", name.id);
    assertEquals("kind", name.kind);
    assertSame(name, binding.binding_name[0]);
    assertEquals(BindingType.nobject, binding.binding_type);
    assertEquals("value", anyPair.id);
    assertNull(anyPair.value);
    assertEquals("dyn", dynPair.id);
    assertNull(dynPair.value);
    assertEquals("member", valueMember.name);
    assertEquals("IDL:member:1.0", valueMember.id);
    assertEquals("IDL:container:1.0", valueMember.defined_in);
    assertEquals("1.0", valueMember.version);
    assertEquals(1, valueMember.access);
    assertEquals("servant", servantObject.servant);
    assertEquals(DefinitionKind._dk_Interface, DefinitionKind._dk_Interface);
    assertEquals(State._ACTIVE, State.ACTIVE.value());
    assertEquals(
        ThreadPolicyValue._SINGLE_THREAD_MODEL, ThreadPolicyValue.SINGLE_THREAD_MODEL.value());
    assertEquals(LifespanPolicyValue._PERSISTENT, LifespanPolicyValue.PERSISTENT.value());
    assertEquals(IdAssignmentPolicyValue._SYSTEM_ID, IdAssignmentPolicyValue.SYSTEM_ID.value());
  }

  @Test
  void holdersExposeValuesAndRejectRuntimeStreaming() {
    POAHolder poaHolder = new POAHolder();
    POAManagerHolder managerHolder = new POAManagerHolder();
    BindingHolder bindingHolder = new BindingHolder(new Binding());
    BindingListHolder bindingListHolder = new BindingListHolder(new Binding[] {new Binding()});
    BindingIteratorHolder iteratorHolder = new BindingIteratorHolder();
    NamingContextHolder contextHolder = new NamingContextHolder();
    NamingContextExtHolder namingHolder = new NamingContextExtHolder();
    DynAnyHolder dynAnyHolder = new DynAnyHolder();

    assertNotNull(bindingHolder.value);
    assertEquals(1, bindingListHolder.value.length);
    assertNull(iteratorHolder.value);
    assertNull(contextHolder.value);
    assertNull(namingHolder.value);
    assertNull(dynAnyHolder.value);
    assertThrows(NO_IMPLEMENT.class, () -> poaHolder._read(null));
    assertThrows(NO_IMPLEMENT.class, () -> poaHolder._write(null));
    assertThrows(NO_IMPLEMENT.class, () -> poaHolder._type());
    assertThrows(NO_IMPLEMENT.class, () -> managerHolder._read(null));
    assertThrows(NO_IMPLEMENT.class, () -> managerHolder._write(null));
    assertThrows(NO_IMPLEMENT.class, () -> managerHolder._type());
    assertThrows(NO_IMPLEMENT.class, () -> bindingHolder._read(null));
    assertThrows(NO_IMPLEMENT.class, () -> bindingHolder._write(null));
    assertThrows(NO_IMPLEMENT.class, () -> bindingHolder._type());
    assertThrows(NO_IMPLEMENT.class, () -> bindingListHolder._read(null));
    assertThrows(NO_IMPLEMENT.class, () -> bindingListHolder._write(null));
    assertThrows(NO_IMPLEMENT.class, () -> bindingListHolder._type());
    assertThrows(NO_IMPLEMENT.class, () -> iteratorHolder._read(null));
    assertThrows(NO_IMPLEMENT.class, () -> iteratorHolder._write(null));
    assertThrows(NO_IMPLEMENT.class, () -> iteratorHolder._type());
    assertThrows(NO_IMPLEMENT.class, () -> contextHolder._read(null));
    assertThrows(NO_IMPLEMENT.class, () -> contextHolder._write(null));
    assertThrows(NO_IMPLEMENT.class, () -> contextHolder._type());
    assertThrows(NO_IMPLEMENT.class, () -> namingHolder._read(null));
    assertThrows(NO_IMPLEMENT.class, () -> namingHolder._write(null));
    assertThrows(NO_IMPLEMENT.class, () -> namingHolder._type());
    assertThrows(NO_IMPLEMENT.class, () -> dynAnyHolder._read(null));
    assertThrows(NO_IMPLEMENT.class, () -> dynAnyHolder._write(null));
    assertThrows(NO_IMPLEMENT.class, () -> dynAnyHolder._type());
  }

  @Test
  void unsupportedRuntimeEntrypointsAreDeterministic() {
    ORB orb = ORB.init(new String[] {"-ORBInitRef"}, new java.util.Properties());
    LocalObject localObject = new LocalObject();
    ObjectImpl stub =
        new ObjectImpl() {
          @Override
          public String[] _ids() {
            return new String[] {"IDL:fixture/Echo:1.0"};
          }
        };

    assertArrayEquals(new String[0], orb.list_initial_services());
    assertThrows(NO_IMPLEMENT.class, orb::create_any);
    assertThrows(NO_IMPLEMENT.class, () -> orb.get_primitive_tc(TCKind.tk_long));
    assertThrows(NO_IMPLEMENT.class, () -> orb.object_to_string(null));
    assertThrows(NO_IMPLEMENT.class, () -> orb.string_to_object(""));
    assertThrows(NO_IMPLEMENT.class, () -> orb.resolve_initial_references("NameService"));
    assertThrows(NO_IMPLEMENT.class, orb::run);
    assertThrows(NO_IMPLEMENT.class, () -> orb.shutdown(false));
    assertThrows(NO_IMPLEMENT.class, orb::destroy);
    assertFalse(localObject._non_existent());
    assertTrue(localObject._is_equivalent(localObject));
    assertEquals(0, localObject._hash(0));
    assertTrue(localObject._hash(7) >= 0);
    assertArrayEquals(new DomainManager[0], localObject._get_domain_managers());
    assertThrows(NO_IMPLEMENT.class, () -> localObject._is_a("IDL:x:1.0"));
    assertThrows(NO_IMPLEMENT.class, () -> localObject._request("op"));
    assertThrows(NO_IMPLEMENT.class, () -> localObject._create_request(null, "op", null, null));
    assertThrows(
        NO_IMPLEMENT.class, () -> localObject._create_request(null, "op", null, null, null, null));
    assertThrows(NO_IMPLEMENT.class, () -> localObject._get_policy(1));
    assertThrows(NO_IMPLEMENT.class, () -> localObject._set_policy_override(null, null));
    assertThrows(BAD_INV_ORDER.class, stub::_get_delegate);
    assertArrayEquals(new String[] {"IDL:fixture/Echo:1.0"}, stub._ids());
  }

  @Test
  void objectImplForwardsDelegateOperations() throws Exception {
    ObjectImpl stub =
        new ObjectImpl() {
          @Override
          public String[] _ids() {
            return new String[] {"IDL:fixture/Echo:1.0"};
          }
        };
    InputStream input = input();
    OutputStream output = output(input);
    Delegate delegate =
        new Delegate() {
          @Override
          public boolean is_a(Object self, String repositoryIdentifier) {
            return repositoryIdentifier.equals(stub._ids()[0]);
          }

          @Override
          public boolean is_equivalent(Object self, Object other) {
            return self == other;
          }

          @Override
          public boolean non_existent(Object self) {
            return false;
          }

          @Override
          public int hash(Object self, int maximum) {
            return 4;
          }

          @Override
          public OutputStream request(Object self, String operation, boolean responseExpected) {
            return output;
          }

          @Override
          public InputStream invoke(Object self, OutputStream output) {
            return input;
          }

          @Override
          public void releaseReply(Object self, InputStream input) {}
        };

    stub._set_delegate(delegate);

    assertSame(delegate, stub._get_delegate());
    assertTrue(stub._is_a("IDL:fixture/Echo:1.0"));
    assertTrue(stub._is_equivalent(stub));
    assertFalse(stub._non_existent());
    assertEquals(4, stub._hash(10));
    assertSame(output, stub._request("say", true));
    assertSame(input, stub._invoke(output));
    stub._releaseReply(input);
    assertArrayEquals(new DomainManager[0], stub._get_domain_managers());
    assertThrows(NO_IMPLEMENT.class, () -> stub._request("op"));
    assertThrows(NO_IMPLEMENT.class, () -> stub._create_request(null, "op", null, null));
    assertThrows(
        NO_IMPLEMENT.class, () -> stub._create_request(null, "op", null, null, null, null));
    assertThrows(NO_IMPLEMENT.class, () -> stub._get_policy(1));
    assertThrows(NO_IMPLEMENT.class, () -> stub._set_policy_override(null, null));
  }

  @Test
  void servantFallbackMethodsAreDeterministic() {
    Servant servant =
        new Servant() {
          @Override
          public String[] _all_interfaces(org.omg.PortableServer.POA poa, byte[] objectId) {
            return new String[] {"IDL:fixture/Echo:1.0"};
          }

          @Override
          public OutputStream _invoke(
              String operation, InputStream input, org.omg.CORBA.portable.ResponseHandler handler) {
            return null;
          }
        };

    assertArrayEquals(new String[] {"IDL:fixture/Echo:1.0"}, servant._all_interfaces(null, null));
    assertThrows(NO_IMPLEMENT.class, servant::_this_object);
    assertThrows(NO_IMPLEMENT.class, () -> servant._this_object(ORB.init()));
    assertThrows(NO_IMPLEMENT.class, servant::_default_POA);
    assertThrows(NO_IMPLEMENT.class, servant::_poa);
    assertThrows(NO_IMPLEMENT.class, servant::_object_id);
  }

  @Test
  void checkedCompatibilityExceptionsExposeFields() {
    NameComponent[] rest = {new NameComponent("missing", "")};
    NotFound notFound = new NotFound(NotFoundReason.missing_node, rest);
    CannotProceed cannotProceed = new CannotProceed(null, rest);
    InvalidPolicy invalidPolicy = new InvalidPolicy((short) 3);
    ForwardRequest forwardRequest = new ForwardRequest(null);
    DuplicateName duplicateName = new DuplicateName("interceptor");
    ApplicationException applicationException = new ApplicationException("IDL:x:1.0", input());
    RemarshalException remarshalException = new RemarshalException();

    assertSame(NotFoundReason.missing_node, notFound.why);
    assertSame(rest, notFound.rest_of_name);
    assertSame(rest, cannotProceed.rest_of_name);
    assertEquals(3, invalidPolicy.index);
    assertInstanceOf(UserException.class, forwardRequest);
    assertEquals("interceptor", duplicateName.name);
    assertEquals("IDL:x:1.0", applicationException.getId());
    assertNotNull(applicationException.getInputStream());
    assertInstanceOf(Exception.class, remarshalException);
    assertEquals(POAHelper.id(), "IDL:omg.org/PortableServer/POA:2.3");
    assertNull(POAHelper.narrow(null));
    assertEquals(NamingContextExtHelper.id(), "IDL:omg.org/CosNaming/NamingContextExt:1.0");
    assertNull(NamingContextExtHelper.narrow(null));
    assertEquals(
        org.omg.CosNaming.NamingContextHelper.id(), "IDL:omg.org/CosNaming/NamingContext:1.0");
    assertNull(org.omg.CosNaming.NamingContextHelper.narrow(null));
  }

  @Test
  void checkedExceptionConstructorsAreCovered() {
    assertNotNull(new Bounds());
    assertNotNull(new Bounds("bounds"));
    assertNotNull(new org.omg.CORBA.ORBPackage.InvalidName());
    assertNotNull(new org.omg.CORBA.ORBPackage.InvalidName("bad"));
    assertNotNull(new org.omg.CORBA.TypeCodePackage.BadKind());
    assertNotNull(new org.omg.CORBA.TypeCodePackage.BadKind("bad"));
    assertNotNull(new org.omg.CORBA.TypeCodePackage.Bounds());
    assertNotNull(new org.omg.CORBA.TypeCodePackage.Bounds("bounds"));
    assertNotNull(new org.omg.PortableServer.POAManagerPackage.AdapterInactive());
    assertNotNull(new org.omg.PortableServer.POAPackage.AdapterAlreadyExists());
    assertNotNull(new org.omg.PortableServer.POAPackage.AdapterNonExistent());
    assertNotNull(new org.omg.PortableServer.POAPackage.NoServant());
    assertNotNull(new org.omg.PortableServer.POAPackage.ObjectAlreadyActive());
    assertNotNull(new org.omg.PortableServer.POAPackage.ObjectNotActive());
    assertNotNull(new org.omg.PortableServer.POAPackage.ServantAlreadyActive());
    assertNotNull(new org.omg.PortableServer.POAPackage.ServantNotActive());
    assertNotNull(new org.omg.PortableServer.POAPackage.WrongAdapter());
    assertNotNull(new org.omg.PortableServer.POAPackage.WrongPolicy());
    assertNotNull(new org.omg.DynamicAny.DynAnyFactoryPackage.InconsistentTypeCode());
    assertNotNull(new org.omg.DynamicAny.DynAnyPackage.InvalidValue());
    assertNotNull(new org.omg.DynamicAny.DynAnyPackage.TypeMismatch());
    assertNotNull(new org.omg.CosNaming.NamingContextPackage.AlreadyBound());
    assertNotNull(new org.omg.CosNaming.NamingContextPackage.InvalidName());
    assertNotNull(new org.omg.CosNaming.NamingContextPackage.NotEmpty());
    assertNotNull(new org.omg.CosNaming.NamingContextExtPackage.InvalidAddress());
  }

  private void compile(String sourcePath, String source) throws Exception {
    Path sourceRoot = Files.createDirectories(tempDir.resolve("source-compat"));
    Path outputRoot = Files.createDirectories(tempDir.resolve("classes"));
    Path sourceFile = sourceRoot.resolve(sourcePath);
    Path parent = sourceFile.getParent();
    assertNotNull(parent);
    Files.createDirectories(parent);
    Files.writeString(sourceFile, source, StandardCharsets.UTF_8);

    List<String> arguments = new ArrayList<>();
    arguments.add("-classpath");
    arguments.add(System.getProperty("java.class.path"));
    arguments.add("-d");
    arguments.add(outputRoot.toString());
    arguments.add(sourceFile.toString());

    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    assertNotNull(compiler, "Source compatibility test requires a JDK compiler");
    assertEquals(0, compiler.run(null, null, null, arguments.toArray(String[]::new)));
  }

  private static InputStream input() {
    return new InputStream() {
      @Override
      public boolean read_boolean() {
        return false;
      }

      @Override
      public char read_char() {
        return 0;
      }

      @Override
      public byte read_octet() {
        return 0;
      }

      @Override
      public short read_short() {
        return 0;
      }

      @Override
      public short read_ushort() {
        return 0;
      }

      @Override
      public int read_long() {
        return 0;
      }

      @Override
      public int read_ulong() {
        return 0;
      }

      @Override
      public long read_longlong() {
        return 0;
      }

      @Override
      public float read_float() {
        return 0;
      }

      @Override
      public double read_double() {
        return 0;
      }

      @Override
      public String read_string() {
        return "";
      }

      @Override
      public Any read_any() {
        return null;
      }

      @Override
      public Object read_Object() {
        return null;
      }
    };
  }

  private static OutputStream output(InputStream input) {
    return new OutputStream() {
      @Override
      public void write_boolean(boolean value) {}

      @Override
      public void write_char(char value) {}

      @Override
      public void write_octet(byte value) {}

      @Override
      public void write_short(short value) {}

      @Override
      public void write_ushort(short value) {}

      @Override
      public void write_long(int value) {}

      @Override
      public void write_ulong(int value) {}

      @Override
      public void write_longlong(long value) {}

      @Override
      public void write_float(float value) {}

      @Override
      public void write_double(double value) {}

      @Override
      public void write_string(String value) {}

      @Override
      public void write_any(Any value) {}

      @Override
      public void write_Object(Object value) {}

      @Override
      public InputStream create_input_stream() {
        return input;
      }
    };
  }
}
