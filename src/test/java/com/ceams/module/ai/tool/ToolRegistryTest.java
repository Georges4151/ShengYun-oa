package com.ceams.module.ai.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** 工具注册表与白名单校验测试。 */
class ToolRegistryTest {

    private static final List<String> EXPECTED_NAMES =
            List.of(
                    "queryProjectSummary",
                    "queryReceivable",
                    "queryExpenseSummary",
                    "queryCashflow",
                    "queryPendingApprovals",
                    "queryBankUnmatched",
                    "queryBankMatchCandidates",
                    "queryBudgetUsage",
                    "querySealUsage");

    @Test
    void registersAllAllowedTools() {
        ToolRegistry registry = registryForAllAllowedTools();
        assertEquals(9, registry.all().size());
        for (String name : EXPECTED_NAMES) {
            assertTrue(registry.find(name).isPresent(), "缺少工具：" + name);
        }
    }

    @Test
    void definitionsHaveOllamaFunctionStructure() {
        ToolRegistry registry = registryForAllAllowedTools();
        List<Map<String, Object>> definitions = registry.definitions();
        assertEquals(9, definitions.size());
        for (Map<String, Object> definition : definitions) {
            assertEquals("function", definition.get("type"));
            @SuppressWarnings("unchecked")
            Map<String, Object> function = (Map<String, Object>) definition.get("function");
            assertTrue(function.get("name") instanceof String);
            assertFalse(((String) function.get("name")).isBlank());
            assertTrue(function.get("description") instanceof String);
            @SuppressWarnings("unchecked")
            Map<String, Object> parameters = (Map<String, Object>) function.get("parameters");
            assertEquals("object", parameters.get("type"));
            assertTrue(parameters.get("properties") instanceof Map);
        }
    }

    @Test
    void rejectsUnknownToolName() {
        ToolRegistry registry = new ToolRegistry(List.of(fakeTool("queryDangerousSql")));
        IllegalStateException ex = assertThrows(IllegalStateException.class, registry::validate);
        assertTrue(ex.getMessage().contains("queryDangerousSql"));
    }

    @Test
    void rejectsBlankToolName() {
        ToolRegistry registry = new ToolRegistry(List.of(fakeTool("  ")));
        assertThrows(IllegalStateException.class, registry::validate);
    }

    @Test
    void rejectsDuplicateToolName() {
        ToolRegistry registry =
                new ToolRegistry(
                        List.of(fakeTool("queryProjectSummary"), fakeTool("queryProjectSummary")));
        IllegalStateException ex = assertThrows(IllegalStateException.class, registry::validate);
        assertTrue(ex.getMessage().contains("重复"));
    }

    @Test
    void findReturnsEmptyForUnknownName() {
        ToolRegistry registry = registryForAllAllowedTools();
        assertTrue(registry.find("queryUnknown").isEmpty());
    }

    private ToolRegistry registryForAllAllowedTools() {
        List<ReadOnlyTool> tools = new ArrayList<>();
        for (String name : EXPECTED_NAMES) {
            tools.add(fakeTool(name));
        }
        ToolRegistry registry = new ToolRegistry(tools);
        registry.validate();
        return registry;
    }

    private ReadOnlyTool fakeTool(String name) {
        return new ReadOnlyTool() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public String description() {
                return "测试用只读工具";
            }

            @Override
            public Map<String, Object> parameterSchema() {
                return ToolSchemas.object(Map.of("keyword", ToolSchemas.text("关键词")), "keyword");
            }

            @Override
            public ToolResult execute(Map<String, Object> args) {
                return ToolResult.empty();
            }
        };
    }
}
