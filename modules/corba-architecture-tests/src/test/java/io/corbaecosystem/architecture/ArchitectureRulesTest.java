package io.corbaecosystem.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Repository-wide architectural boundary checks. */
@Tag("architecture")
final class ArchitectureRulesTest {

  @BeforeAll
  static void allowEmptyRulesDuringScaffoldPhase() {
    ArchConfiguration.get().setProperty("archRule.failOnEmptyShould", "false");
  }

  @Test
  void protocolModulesMustNotDependOnOrbCoreOrPoa() {
    JavaClasses classes = new ClassFileImporter().importPackages("io.corbaecosystem");

    noClasses()
        .that()
        .resideInAnyPackage("..cdr..", "..giop..", "..iiop..", "..ior..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..orb..", "..poa..")
        .check(classes);
  }

  @Test
  void coreProtocolModulesMustNotUseReflection() {
    JavaClasses classes = new ClassFileImporter().importPackages("io.corbaecosystem");

    noClasses()
        .that()
        .resideInAnyPackage("..cdr..", "..giop..", "..iiop..", "..ior..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("java.lang.reflect..")
        .check(classes);
  }

  @Test
  void normalMarshallingModulesMustNotUseJavaSerialization() {
    JavaClasses classes = new ClassFileImporter().importPackages("io.corbaecosystem");

    noClasses()
        .that()
        .resideInAnyPackage("..cdr..", "..codegen..", "..giop..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("java.io..")
        .check(classes);
  }
}
