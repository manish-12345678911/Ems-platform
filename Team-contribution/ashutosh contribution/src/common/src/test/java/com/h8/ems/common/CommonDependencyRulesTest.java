package com.h8.ems.common;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ArchUnit test enforcing that the common module has NO Spring, JPA, Kafka, or Redis dependencies.
 */
class CommonDependencyRulesTest {

    private final JavaClasses commonClasses = new ClassFileImporter()
            .importPackages("com.h8.ems.common");

    @Test
    void commonMustNotDependOnSpring() {
        ArchRule rule = noClasses()
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework..");
        rule.check(commonClasses);
    }

    @Test
    void commonMustNotDependOnJpa() {
        ArchRule rule = noClasses()
                .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..", "javax.persistence..");
        rule.check(commonClasses);
    }

    @Test
    void commonMustNotDependOnKafka() {
        ArchRule rule = noClasses()
                .should().dependOnClassesThat().resideInAnyPackage("org.apache.kafka..", "org.springframework.kafka..");
        rule.check(commonClasses);
    }

    @Test
    void commonMustNotDependOnRedis() {
        ArchRule rule = noClasses()
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework.data.redis..", "io.lettuce..", "redis.clients..");
        rule.check(commonClasses);
    }
}
