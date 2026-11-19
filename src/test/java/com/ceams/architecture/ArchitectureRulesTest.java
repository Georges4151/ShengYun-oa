package com.ceams.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * 架构约束测试（ArchUnit）。
 *
 * <p>验收规则（docs/STAGE3_PLAN.md §6.2.6）：
 *
 * <ul>
 *   <li>Controller 不得直接依赖 Mapper（数据访问必须经过 Service 层）；
 *   <li>Entity 不得出现在 Controller（接口出入参必须使用 DTO/VO，经 Converter 转换）。
 * </ul>
 */
@AnalyzeClasses(packages = "com.ceams")
class ArchitectureRulesTest {

    @ArchTest
    static final ArchRule controller不得直接依赖Mapper =
            noClasses()
                    .that()
                    .resideInAPackage("..controller..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..mapper..")
                    .because(
                            "Controller 层不得直接依赖 Mapper 层，数据访问必须通过 Service 层，"
                                    + "以保证分层边界清晰、事务与鉴权逻辑集中在 Service 中统一控制");

    @ArchTest
    static final ArchRule entity不得出现在Controller =
            noClasses()
                    .that()
                    .resideInAPackage("..controller..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..entity..")
                    .because(
                            "Entity（持久化实体）不得出现在 Controller 层，接口出入参必须使用 DTO/VO，"
                                    + "由 Converter 完成与 Entity 的转换，避免持久化结构与接口契约耦合");
}
