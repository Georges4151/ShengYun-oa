package com.ceams.module.bpm.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.bpm.enums.BpmAssigneeSource;
import com.ceams.module.bpm.enums.BpmNodeType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 模板 JSON 结构校验：唯一 key、唯一 START、END 默认出口、禁止成环。 */
class BpmSchemaParserTest {

    private BpmSchemaParser parser;

    @BeforeEach
    void setUp() {
        parser = new BpmSchemaParser(new ObjectMapper());
    }

    @Test
    void 合法模板解析并归一化审批模式() {
        String json =
                """
                {"nodes":[
                  {"nodeKey":"start","seqNo":1,"nodeType":"START"},
                  {"nodeKey":"dept","nodeName":"部门审批","seqNo":2,"nodeType":"APPROVAL",
                   "approveMode":"ANY","assignees":[{"type":"ROLE","value":"FINANCE"}],"dueHours":12},
                  {"nodeKey":"end","seqNo":3,"nodeType":"END"}
                ]}
                """;

        BpmSchema schema = parser.parse(json);

        assertThat(schema.getNodes()).hasSize(3);
        assertThat(schema.getNodes().get(1).getNodeType()).isEqualTo(BpmNodeType.OR_SIGN);
        assertThat(schema.getNodes().get(1).getAssignees().get(0).getType())
                .isEqualTo(BpmAssigneeSource.ROLE);
        assertThat(schema.getNodes().get(1).getDueHours()).isEqualTo(12);
    }

    @Test
    void 节点编码重复时拒绝() {
        String json = nodes(
                "{\"nodeKey\":\"start\",\"seqNo\":1,\"nodeType\":\"START\"}",
                "{\"nodeKey\":\"start\",\"seqNo\":2,\"nodeType\":\"END\"}");

        assertInvalid(json);
    }

    @Test
    void 缺少START节点时拒绝() {
        String json = nodes(
                "{\"nodeKey\":\"approve\",\"seqNo\":1,\"nodeType\":\"SINGLE\",\"assignees\":[{\"type\":\"USER\",\"value\":\"1\"}]}",
                "{\"nodeKey\":\"end\",\"seqNo\":2,\"nodeType\":\"END\"}");

        assertInvalid(json);
    }

    @Test
    void 缺少END默认出口时拒绝() {
        String json = nodes(
                "{\"nodeKey\":\"start\",\"seqNo\":1,\"nodeType\":\"START\"}",
                "{\"nodeKey\":\"approve\",\"seqNo\":2,\"nodeType\":\"SINGLE\",\"assignees\":[{\"type\":\"USER\",\"value\":\"1\"}]}");

        assertInvalid(json);
    }

    @Test
    void 显式后继成环时拒绝() {
        String json =
                """
                {"nodes":[
                  {"nodeKey":"start","seqNo":1,"nodeType":"START","nextNodeKey":"a"},
                  {"nodeKey":"a","seqNo":2,"nodeType":"SINGLE","nextNodeKey":"b",
                   "assignees":[{"type":"USER","value":"1"}]},
                  {"nodeKey":"b","seqNo":3,"nodeType":"SINGLE","nextNodeKey":"a",
                   "assignees":[{"type":"USER","value":"1"}]},
                  {"nodeKey":"end","seqNo":4,"nodeType":"END"}
                ]}
                """;

        assertInvalid(json);
    }

    @Test
    void 条件节点缺少默认出口时拒绝() {
        String json =
                """
                {"nodes":[
                  {"nodeKey":"start","seqNo":1,"nodeType":"START"},
                  {"nodeKey":"cond","seqNo":2,"nodeType":"CONDITION",
                   "conditions":[{"field":"amount","operator":"GT","value":"5000","targetNodeKey":"high"}]},
                  {"nodeKey":"high","seqNo":3,"nodeType":"SINGLE",
                   "assignees":[{"type":"USER","value":"1"}]},
                  {"nodeKey":"end","seqNo":4,"nodeType":"END"}
                ]}
                """;

        assertInvalid(json);
    }

    @Test
    void 审批节点缺少审批人时拒绝() {
        String json = nodes(
                "{\"nodeKey\":\"start\",\"seqNo\":1,\"nodeType\":\"START\"}",
                "{\"nodeKey\":\"approve\",\"seqNo\":2,\"nodeType\":\"SINGLE\"}",
                "{\"nodeKey\":\"end\",\"seqNo\":3,\"nodeType\":\"END\"}");

        assertInvalid(json);
    }

    @Test
    void 解析自动通过配置() {
        String json =
                """
                {"nodes":[
                  {"nodeKey":"start","seqNo":1,"nodeType":"START"},
                  {"nodeKey":"dept","nodeName":"部门审批","seqNo":2,"nodeType":"SINGLE",
                   "assignees":[{"type":"USER","value":"1"}],
                   "autoApproveIfInitiator":true,
                   "autoApproveWhen":[{"field":"amount","operator":"LE","value":"500"}]},
                  {"nodeKey":"end","seqNo":3,"nodeType":"END"}
                ]}
                """;

        BpmSchemaNode node = parser.parse(json).getNodes().get(1);

        assertThat(node.getAutoApproveIfInitiator()).isTrue();
        assertThat(node.getAutoApproveConditions()).hasSize(1);
        assertThat(node.getAutoApproveConditions().get(0).getField()).isEqualTo("amount");
        assertThat(node.getAutoApproveConditions().get(0).getOperator()).isEqualTo("LE");
    }

    @Test
    void 自动通过条件缺少字段时拒绝() {
        String json = nodes(
                "{\"nodeKey\":\"start\",\"seqNo\":1,\"nodeType\":\"START\"}",
                "{\"nodeKey\":\"dept\",\"seqNo\":2,\"nodeType\":\"SINGLE\","
                        + "\"assignees\":[{\"type\":\"USER\",\"value\":\"1\"}],"
                        + "\"autoApproveWhen\":[{\"operator\":\"LE\",\"value\":\"500\"}]}",
                "{\"nodeKey\":\"end\",\"seqNo\":3,\"nodeType\":\"END\"}");

        assertInvalid(json);
    }

    private String nodes(String... nodeJson) {
        return "{\"nodes\":[" + String.join(",", nodeJson) + "]}";
    }

    private void assertInvalid(String json) {
        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BPM_SCHEMA_INVALID);
    }
}
