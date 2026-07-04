package org.dromara.portal.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 限界上下文边界纪律（docs/architecture/bounded-contexts.md §7），构建期强制：
 * 1. 内容 BC（appcenter/resources/forum）之间禁止相互依赖；
 * 2. 内核对内容 BC 只允许依赖其 service 接口（聚合统计），禁止触碰 domain/mapper/impl；
 * 3. 任何内容 BC 不得依赖内核的 service.impl。
 * 运行方式：mvn -pl ruoyi-modules/ruoyi-portal -am -DskipTests=false test
 */
class BcBoundaryTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("org.dromara.portal");
    }

    @Test
    void appcenter_should_not_depend_on_other_content_bcs() {
        noClasses().that().resideInAPackage("org.dromara.portal.appcenter..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "org.dromara.portal.resources..", "org.dromara.portal.forum..")
            .check(classes);
    }

    @Test
    void resources_should_not_depend_on_other_content_bcs() {
        noClasses().that().resideInAPackage("org.dromara.portal.resources..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "org.dromara.portal.appcenter..", "org.dromara.portal.forum..")
            .check(classes);
    }

    @Test
    void forum_should_not_depend_on_other_content_bcs() {
        noClasses().that().resideInAPackage("org.dromara.portal.forum..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "org.dromara.portal.appcenter..", "org.dromara.portal.resources..")
            .check(classes);
    }

    @Test
    void kernel_should_only_touch_content_bc_service_interfaces() {
        noClasses().that().resideInAPackage("org.dromara.portal.kernel..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "org.dromara.portal.appcenter.domain..", "org.dromara.portal.appcenter.mapper..", "org.dromara.portal.appcenter.service.impl..",
                "org.dromara.portal.resources.domain..", "org.dromara.portal.resources.mapper..", "org.dromara.portal.resources.service.impl..",
                "org.dromara.portal.forum.domain..", "org.dromara.portal.forum.mapper..", "org.dromara.portal.forum.service.impl..")
            .check(classes);
    }

    @Test
    void content_bcs_should_not_depend_on_kernel_impl() {
        noClasses().that().resideInAnyPackage(
                "org.dromara.portal.appcenter..", "org.dromara.portal.resources..", "org.dromara.portal.forum..")
            .should().dependOnClassesThat().resideInAPackage("org.dromara.portal.kernel.service.impl..")
            .check(classes);
    }
}
