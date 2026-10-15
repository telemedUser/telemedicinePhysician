package com.telemedicine.common.core;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Keeps the foundation module framework-free.
 *
 * <p>Every other shared library and all seventeen services depend on core. The moment it pulls in
 * Spring, the error catalogue and PHI helpers stop being usable from plain unit tests, from the
 * Python-adjacent tooling, and from any future non-Spring runtime.
 */
@AnalyzeClasses(
        packages = "com.telemedicine.common.core",
        importOptions = {ImportOption.DoNotIncludeTests.class})
class CoreArchitectureTest {

    @ArchTest
    static final ArchRule coreDoesNotDependOnSpring =
            noClasses()
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("org.springframework..")
                    .because("java-common-core must stay usable without a Spring container");

    @ArchTest
    static final ArchRule coreDoesNotDependOnPersistence =
            noClasses()
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("jakarta.persistence..", "jakarta.servlet..")
                    .because("java-common-core carries no transport or storage concerns");

    @ArchTest
    static final ArchRule noStandardStreamLogging =
            noClasses()
                    .should()
                    .callMethod(System.class, "exit", int.class)
                    .because("a shared library must never terminate its host process");
}
