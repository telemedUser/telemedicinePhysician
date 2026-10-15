package com.telemedicine.common.testing.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class TelemedicineArchRulesTest {

    private static final JavaClasses CORE =
            new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages("com.telemedicine.common.core");

    @Test
    void coreMustNotDependOnSpring() {
        TelemedicineArchRules.coreMustNotDependOnSpring().check(CORE);
    }

    @Test
    void noSystemOutInTestingModule() {
        JavaClasses testing =
                new ClassFileImporter()
                        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                        .importPackages("com.telemedicine.common.testing");
        noClasses().should().accessField(System.class, "out").check(testing);
    }
}
