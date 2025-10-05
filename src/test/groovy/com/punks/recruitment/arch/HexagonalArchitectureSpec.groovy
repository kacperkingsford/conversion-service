package com.punks.recruitment.arch

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition
import spock.lang.Specification

class HexagonalArchitectureSpec extends Specification {

    def importedClasses = new ClassFileImporter(
            [ImportOption.Predefined.DO_NOT_INCLUDE_TESTS]
    ).importPackages("com.punks.recruitment")

    def "Domain should not depend on Application or Infrastructure"() {
        given:
        def rule = ArchRuleDefinition.noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..application..", "..infrastructure..")

        expect:
        rule.check(importedClasses)
    }

    def "Infrastructure should not be a dependency for Domain or Application"() {
        given:
        def rule = ArchRuleDefinition.noClasses()
                .that().resideInAnyPackage("..domain..", "..application..")
                .should().dependOnClassesThat()
                .resideInAPackage("..infrastructure..")

        expect:
        rule.check(importedClasses)
    }
}
