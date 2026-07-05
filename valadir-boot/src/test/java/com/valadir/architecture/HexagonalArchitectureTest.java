package com.valadir.architecture;

import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import java.util.Arrays;
import java.util.stream.Stream;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

/**
 * Executable specification of the hexagonal architecture: a violation breaks the build.
 * Covers the dependency rule (domain ← application ← infrastructure ← composition root),
 * the framework isolation of the inner layers, and the naming conventions.
 *
 * <p>Package roots do not mirror the Maven module names — infrastructure lives under
 * {@code web}/{@code persistence}/{@code security}/{@code notifications} and the composition
 * root under {@code config}/{@code scheduler}/{@code logging}, with no shared {@code infrastructure.*}
 * prefix. Hence the layering uses explicit {@code noClasses} rules, not {@code layeredArchitecture}.
 *
 * <p>{@code common} is intentionally absent from the "outer layer" arrays: it holds shared ports
 * ({@code RateLimiter}) that domain and application may legitimately depend on.
 */
@AnalyzeClasses(packages = "com.valadir", importOptions = DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    private static final String DOMAIN = "com.valadir.domain..";
    private static final String APPLICATION = "com.valadir.application..";
    private static final String WEB = "com.valadir.web..";

    private static final String[] INFRASTRUCTURE = {
        "com.valadir.web..",
        "com.valadir.persistence..",
        "com.valadir.security..",
        "com.valadir.notifications.."
    };

    private static final String[] COMPOSITION_ROOT = {
        "com.valadir.config..",
        "com.valadir.scheduler..",
        "com.valadir.logging.."
    };

    private static final String[] OUTER_THAN_DOMAIN =
        Stream.of(new String[]{APPLICATION}, INFRASTRUCTURE, COMPOSITION_ROOT)
            .flatMap(Arrays::stream)
            .toArray(String[]::new);

    private static final String[] OUTER_THAN_APPLICATION =
        Stream.of(INFRASTRUCTURE, COMPOSITION_ROOT)
            .flatMap(Arrays::stream)
            .toArray(String[]::new);

    private static final String[] FRAMEWORKS = {
        "org.springframework..",
        "jakarta..",
        "javax.persistence..",
        "com.fasterxml.jackson..",
        "org.hibernate.."
    };

    @ArchTest
    static final ArchRule domain_does_not_depend_on_outer_layers =
        noClasses().that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(OUTER_THAN_DOMAIN);

    @ArchTest
    static final ArchRule application_does_not_depend_on_infrastructure_or_boot =
        noClasses().that().resideInAPackage(APPLICATION)
            .should().dependOnClassesThat().resideInAnyPackage(OUTER_THAN_APPLICATION);

    // The application boundary speaks primitives and application DTOs only. Commands, results and the
    // driving ports that expose them must never carry a domain type (value object, entity, aggregate),
    // so any driving adapter can call a use case without depending on the domain.
    @ArchTest
    static final ArchRule application_boundary_carries_no_domain_types =
        noClasses().that().resideInAnyPackage(
                "com.valadir.application.command..",
                "com.valadir.application.result..",
                "com.valadir.application.port.in..")
            .should().dependOnClassesThat().resideInAPackage(DOMAIN);

    @ArchTest
    static final ArchRule domain_and_application_are_framework_free =
        noClasses().that().resideInAnyPackage(DOMAIN, APPLICATION)
            .should().dependOnClassesThat().resideInAnyPackage(FRAMEWORKS);

    @ArchTest
    static final ArchRule controllers_do_not_depend_on_the_domain =
        noClasses().that().resideInAPackage(WEB)
            .should().dependOnClassesThat().resideInAPackage(DOMAIN);

    @ArchTest
    static final ArchRule request_response_dtos_do_not_reference_other_layers =
        noClasses().that().resideInAPackage("com.valadir.web.dto..")
            .should().dependOnClassesThat().resideInAnyPackage(DOMAIN, APPLICATION);

    @ArchTest
    static final ArchRule jpa_entities_are_named_with_the_entity_suffix =
        classes().that().areAnnotatedWith("jakarta.persistence.Entity")
            .should().haveSimpleNameEndingWith("Entity");

    @ArchTest
    static final ArchRule entity_named_classes_stay_in_the_persistence_layer =
        classes().that().haveSimpleNameEndingWith("Entity")
            .should().resideInAPackage("com.valadir.persistence.entity..");

    @ArchTest
    static final ArchRule adapters_reside_in_an_adapter_package =
        classes().that().haveSimpleNameEndingWith("Adapter")
            .should().resideInAPackage("..adapter..");

    // An adapter package holds only adapters: driven ones named *Adapter (the [PortName][Technology]Adapter convention)
    // and driving ones named *Controller. Nothing else — services, mappers, helpers — leaks into an adapter package.
    // Scoped to top-level classes: nested types are encapsulated implementation details of their adapter,
    // not residents of the package.
    @ArchTest
    static final ArchRule adapter_packages_contain_only_adapters =
        classes().that().resideInAPackage("..adapter..")
            .and().areTopLevelClasses()
            .should().haveSimpleNameEndingWith("Adapter")
            .orShould().haveSimpleNameEndingWith("Controller");

    @ArchTest
    static final ArchRule mappers_are_not_named_after_the_persistence_technology =
        noMethods().that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Mapper")
            .should().haveNameMatching(".*([Jj]pa|[Hh]ibernate).*");

    @ArchTest
    static final ArchRule ports_do_not_contain_the_word_port =
        noClasses().that().resideInAPackage("com.valadir.application.port..")
            .should().haveSimpleNameContaining("Port");
}
