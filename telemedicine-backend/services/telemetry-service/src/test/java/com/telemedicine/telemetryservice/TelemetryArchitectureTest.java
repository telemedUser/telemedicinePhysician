package com.telemedicine.telemetryservice;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
        packages = "com.telemedicine.telemetryservice",
        importOptions = ImportOption.DoNotIncludeTests.class)
class TelemetryArchitectureTest {

    @ArchTest
    static final ArchRule telemetryMustNotDependOnVideoService =
            noClasses()
                    .that()
                    .resideInAPackage("com.telemedicine.telemetryservice..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("com.telemedicine.videoservice..")
                    .because("telemetry-service must stay independently deployable from video-service");
}
