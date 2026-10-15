package com.telemedicine.common.testing.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.telemedicine.common.core.dto.ApiResponse;
import com.telemedicine.common.core.phi.Phi;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchRule;

/** Reusable ArchUnit rules every Telemedicine service can import into its architecture tests. */
public final class TelemedicineArchRules {

    private TelemedicineArchRules() {}

    public static ArchRule coreMustNotDependOnSpring() {
        return noClasses()
                .that()
                .resideInAPackage("com.telemedicine.common.core..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("org.springframework..")
                .because("java-common-core must stay usable without a Spring container");
    }

    public static ArchRule webMustNotDependOnPersistence() {
        return noClasses()
                .that()
                .resideInAPackage("com.telemedicine.common.web..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "com.telemedicine.common.persistence..", "jakarta.persistence..")
                .because("web concerns must not leak storage dependencies");
    }

    public static ArchRule servicesMustNotDependOnEachOther(String servicePackage) {
        return noClasses()
                .that()
                .resideInAPackage(servicePackage + "..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("com.telemedicine..service..")
                .because(
                        "services must communicate over HTTP and events, not compile-time imports");
    }

    public static ArchRule controllersReturnApiResponse(String servicePackage) {
        return methods()
                .that()
                .areDeclaredInClassesThat()
                .resideInAPackage(servicePackage + "..controller..")
                .and()
                .arePublic()
                .and()
                .doNotHaveName("hashCode")
                .and()
                .doNotHaveName("equals")
                .and()
                .doNotHaveName("toString")
                .should()
                .haveRawReturnType(ApiResponse.class)
                .because("REST controllers must use the shared success envelope");
    }

    public static ArchRule entitiesExtendBaseEntity(String servicePackage) {
        return classes()
                .that()
                .resideInAPackage(servicePackage + "..domain..")
                .and()
                .areAnnotatedWith(jakarta.persistence.Entity.class)
                .should()
                .beAssignableTo(
                        com.tngtech.archunit.base.DescribedPredicate.describe(
                                "extend com.telemedicine.common.persistence.BaseEntity",
                                TelemedicineArchRules::extendsBaseEntity))
                .because("every persisted aggregate uses the shared auditing base class");
    }

    private static boolean extendsBaseEntity(JavaClass type) {
        return type.isAssignableTo("com.telemedicine.common.persistence.BaseEntity");
    }

    public static ArchRule noSystemOut() {
        return noClasses()
                .should()
                .accessField(System.class, "out")
                .orShould()
                .accessField(System.class, "err")
                .because("logging belongs in SLF4J, not standard streams");
    }

    public static ArchRule phiFieldsAreAnnotated(String basePackage) {
        return fields().that()
                .areDeclaredInClassesThat()
                .resideInAPackage(basePackage + "..")
                .and()
                .haveNameMatching(
                        "(?i).*(rut|email|phone|address|mrn|patientName|firstName|lastName).*")
                .should()
                .beAnnotatedWith(Phi.class)
                .because("PHI-typed fields must be explicitly marked for redaction and encryption");
    }
}
