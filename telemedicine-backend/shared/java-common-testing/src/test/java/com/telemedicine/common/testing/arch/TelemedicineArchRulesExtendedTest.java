package com.telemedicine.common.testing.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class TelemedicineArchRulesExtendedTest {

    private static final JavaClasses WEB =
            new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages("com.telemedicine.common.web");

    private static final JavaClasses TESTING =
            new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages("com.telemedicine.common.testing");

    @Test
    void webMustNotDependOnPersistence() {
        TelemedicineArchRules.webMustNotDependOnPersistence().check(WEB);
    }

    @Test
    void servicesMustNotDependOnEachOtherIsConfigurable() {
        TelemedicineArchRules.servicesMustNotDependOnEachOther("com.telemedicine.common.testing")
                .check(TESTING);
    }

    @Test
    void controllersReturnApiResponseRuleIsAvailable() {
        TelemedicineArchRules.controllersReturnApiResponse("com.telemedicine.common.testing")
                .check(TESTING);
    }

    @Test
    void entitiesExtendBaseEntityRuleIsAvailable() {
        TelemedicineArchRules.entitiesExtendBaseEntity("com.telemedicine.common.testing")
                .check(TESTING);
    }

    @Test
    void phiFieldsMustBeAnnotatedInTestingFixtures() {
        TelemedicineArchRules.phiFieldsAreAnnotated("com.telemedicine.common.testing.fixtures")
                .check(TESTING);
    }

    @Test
    void noSystemOutInTestingModule() {
        TelemedicineArchRules.noSystemOut().check(TESTING);
    }
}
