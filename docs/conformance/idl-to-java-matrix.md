# IDL to Java Mapping Conformance Matrix

| Mapping area | Clause / section | Requirement IDs | Status | Test IDs | Notes |
|---|---|---|---|---|---|
| modules to packages | I2JAV-13-MODULES | REQ-IDLJ-002 | partial | `IdlJavaMapperTest`, `JavaSourceGeneratorTest`, G6-210 Hello golden-source fixture | G6-160 maps IDL module scopes to deterministic compile-safe Java packages in explicit legacy and modern modes. G6-210 proves the shared Hello IDL fixture end to end. |
| interfaces | I2JAV-13-INTERFACES | REQ-IDLJ-002 | partial | `IdlJavaMapperTest`, `JavaSourceGeneratorTest`, G6-210 Hello golden-source fixture | G6-160 emits compile-safe Java interfaces for the minimal parser subset. G6-210 compares and compiles resource-backed Hello interface goldens. Legacy helper/stub/POA artifacts remain deferred. |
| operations | I2JAV-13-INTERFACES, parameter passing modes | REQ-IDLJ-002 | partial | `IdlJavaMapperTest`, `JavaSourceGeneratorTest`, G6-210 Hello golden-source fixture | G6-160 emits operation method signatures and checked raises clauses. G6-210 covers a fixture-backed string operation. Holder-based parameter passing remains deferred. |
| attributes | I2JAV-13-INTERFACES | REQ-IDLJ-002 | partial | `IdlJavaMapperTest`, `JavaSourceGeneratorTest` | G6-160 emits deterministic getter/setter method signatures for readonly and read/write attributes. |
| structs | I2JAV-13-BASIC | REQ-IDLJ-002 | partial | `IdlJavaMapperTest`, `JavaSourceGeneratorTest` | G6-160 emits compile-safe final value classes for minimal struct declarations. |
| enums | I2JAV-13-BASIC | REQ-IDLJ-002 | partial | `IdlJavaMapperTest`, `JavaSourceGeneratorTest` | G6-160 emits Java enums for minimal IDL enum declarations. |
| constants | I2JAV-13-BASIC | REQ-IDLJ-002 | partial | `IdlJavaMapperTest`, `JavaSourceGeneratorTest` | G6-160 emits deterministic constant holder classes for evaluated semantic constants. |
| helpers | I2JAV-13-HELPERS | REQ-IDLJ-002 | not-started | unassigned | G3 assigns tests. |
| holders | I2JAV-13-BASIC, holder classes | REQ-IDLJ-002 | not-started | unassigned | G3 assigns tests. |
| stubs | I2JAV-13-PORTABILITY, portability stub and skeleton interfaces | REQ-IDLJ-002 | not-started | unassigned | G3 assigns tests. |
| POA skeletons | I2JAV-13-SERVER | REQ-IDLJ-002 | not-started | unassigned | G3 assigns tests. |
| user exceptions | I2JAV-13-EXCEPTIONS | REQ-IDLJ-002 | partial | `IdlJavaMapperTest`, `JavaSourceGeneratorTest` | G6-160 emits checked Java exception classes for minimal IDL exception declarations. Repository IDs and CORBA exception runtime integration remain deferred. |
| static descriptors and codec surfaces | I2JAV-13-PORTABILITY, CORBA-IF-TYPECODE | REQ-IDLJ-004, REQ-NATIVE-002 | partial | `JavaDescriptorSourceGeneratorTest`, `IdlDescriptorTest` | G6-220 emits deterministic static descriptor and compile-only codec source for the Hello slice. Functional CDR string/aggregate marshaling remains deferred to G6-320. |
