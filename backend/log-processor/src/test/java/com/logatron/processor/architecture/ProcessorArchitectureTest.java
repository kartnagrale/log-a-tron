package com.logatron.processor.architecture;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

class ProcessorArchitectureTest {
    private final com.tngtech.archunit.core.domain.JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.logatron.processor");

    @Test void processorPackagesAreFreeOfCycles() {
        slices().matching("com.logatron.processor.(*)..").should().beFreeOfCycles().check(classes);
    }

    @Test void parsersDoNotDependOnTransportPersistenceOrMetadata() {
        noClasses().that().resideInAPackage("..parse..")
                .should().dependOnClassesThat().resideInAnyPackage("..consume..", "..store..", "..metadata..")
                .check(classes);
    }

    @Test void canonicalDomainStagesDoNotDependOnKafka() {
        noClasses().that().resideInAnyPackage("..protect..", "..validate..", "..deduplicate..", "..enrich..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework.kafka..")
                .check(classes);
    }
}