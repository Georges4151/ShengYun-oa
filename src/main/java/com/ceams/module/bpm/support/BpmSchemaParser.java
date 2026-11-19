package com.ceams.module.bpm.support;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.bpm.enums.BpmAssigneeSource;
import com.ceams.module.bpm.enums.BpmNodeType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 流程模板 JSON 解析器。
 *
 * <p>模板 JSON 约定（M1 最小 Schema，不做拖拽画布）：
 *
 * <pre>{@code
 * {
 *   "nodes": [
 *     {"nodeKey":"start","seqNo":1,"nodeType":"START"},
 *     {"nodeKey":"dept","nodeName":"部门审批","seqNo":2,"nodeType":"APPROVAL",
 *      "approveMode":"ALL","assignees":[{"type":"ROLE","value":"FINANCE"}],"dueHours":24},
 *     {"nodeKey":"end","seqNo":3,"nodeType":"END"}
 *   ]
 * }
 * }</pre>
 *
 * <p>结构校验：节点 key/seqNo 唯一、有且仅有一个 START、有且仅有一个 END 且作为默认出口
 * （seqNo 最大）、显式后继与条件出口必须存在、不得成环。APPROVAL 节点按 approveMode
 * 归一化：ALL（默认）=会签，ANY=或签；也兼容直接写 COUNTERSIGN / OR_SIGN / SINGLE / CC。
 */
@Component
@RequiredArgsConstructor
public class BpmSchemaParser {

    private final ObjectMapper objectMapper;

    /** 解析并校验模板 JSON，失败抛 {@link ErrorCode#BPM_SCHEMA_INVALID}。 */
    public BpmSchema parse(String schemaJson) {
        if (!StringUtils.hasText(schemaJson)) {
            throw BusinessException.of(ErrorCode.BPM_SCHEMA_INVALID, "流程模板 JSON 不能为空");
        }
        try {
            JsonNode root = objectMapper.readTree(schemaJson);
            JsonNode nodesNode = root.get("nodes");
            if (nodesNode == null || !nodesNode.isArray() || nodesNode.isEmpty()) {
                throw BusinessException.of(ErrorCode.BPM_SCHEMA_INVALID, "流程模板至少需要一个节点");
            }
            List<BpmSchemaNode> nodes = new ArrayList<>();
            Set<String> keys = new HashSet<>();
            Set<Integer> seqs = new HashSet<>();
            int fallbackSeq = 0;
            for (JsonNode item : nodesNode) {
                fallbackSeq++;
                BpmSchemaNode node = parseNode(item, fallbackSeq);
                if (!keys.add(node.getNodeKey())) {
                    throw BusinessException.of(ErrorCode.BPM_SCHEMA_INVALID, "节点编码重复：" + node.getNodeKey());
                }
                if (!seqs.add(node.getSeqNo())) {
                    throw BusinessException.of(ErrorCode.BPM_SCHEMA_INVALID, "节点顺序号重复：" + node.getSeqNo());
                }
                nodes.add(node);
            }
            nodes.sort(Comparator.comparing(BpmSchemaNode::getSeqNo));
            validateStructure(nodes);
            BpmSchema schema = new BpmSchema();
            schema.setNodes(nodes);
            return schema;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw BusinessException.of(ErrorCode.BPM_SCHEMA_INVALID, "流程模板 JSON 解析失败");
        }
    }

    /** 结构校验：START/END 唯一、默认出口存在、显式跳转目标存在、禁止成环。 */
    private void validateStructure(List<BpmSchemaNode> nodes) {
        int startCount = 0;
        int endCount = 0;
        BpmSchemaNode endNode = null;
        Map<String, BpmSchemaNode> byKey = new LinkedHashMap<>();
        for (BpmSchemaNode node : nodes) {
            byKey.put(node.getNodeKey(), node);
            if (node.getNodeType() == BpmNodeType.START) {
                startCount++;
            }
            if (node.getNodeType() == BpmNodeType.END) {
                endCount++;
                endNode = node;
            }
            if (node.getNodeType() == BpmNodeType.CONDITION
                    && !node.getConditions().isEmpty()
                    && !StringUtils.hasText(node.getDefaultNext())
                    && !StringUtils.hasText(node.getNextNodeKey())) {
                throw BusinessException.of(
                        ErrorCode.BPM_SCHEMA_INVALID, "条件节点 " + node.getNodeKey() + " 缺少默认出口");
            }
        }
        if (startCount != 1) {
            throw BusinessException.of(ErrorCode.BPM_SCHEMA_INVALID, "流程模板必须有且仅有一个 START 节点");
        }
        if (endCount != 1) {
            throw BusinessException.of(ErrorCode.BPM_SCHEMA_INVALID, "流程模板必须有且仅有一个 END 节点作为默认出口");
        }
        BpmSchemaNode lastOrdered = nodes.get(nodes.size() - 1);
        if (endNode == null || endNode.getSeqNo() != lastOrdered.getSeqNo()) {
            throw BusinessException.of(ErrorCode.BPM_SCHEMA_INVALID, "END 节点必须是最后一个节点（默认出口）");
        }

        // 建立有向边：显式 next / defaultNext / 条件出口；未声明显式后继的节点按 seqNo 顺序流转
        Map<String, List<String>> edges = new HashMap<>();
        for (int i = 0; i < nodes.size(); i++) {
            BpmSchemaNode node = nodes.get(i);
            List<String> targets = new ArrayList<>();
            addTarget(targets, node.getNextNodeKey(), byKey, node.getNodeKey());
            addTarget(targets, node.getDefaultNext(), byKey, node.getNodeKey());
            for (BpmSchemaCondition condition : node.getConditions()) {
                addTarget(targets, condition.getTargetNodeKey(), byKey, node.getNodeKey());
            }
            if (targets.isEmpty() && node.getNodeType() != BpmNodeType.END && i + 1 < nodes.size()) {
                targets.add(nodes.get(i + 1).getNodeKey());
            }
            edges.put(node.getNodeKey(), targets);
        }
        detectCycle(byKey.keySet(), edges);
    }

    private void addTarget(
            List<String> targets, String target, Map<String, BpmSchemaNode> byKey, String fromKey) {
        if (!StringUtils.hasText(target)) {
            return;
        }
        if (!byKey.containsKey(target)) {
            throw BusinessException.of(
                    ErrorCode.BPM_SCHEMA_INVALID, "节点 " + fromKey + " 的后继节点不存在：" + target);
        }
        targets.add(target);
    }

    /** DFS 环检测。 */
    private void detectCycle(Set<String> allKeys, Map<String, List<String>> edges) {
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        for (String key : allKeys) {
            if (hasCycle(key, edges, visited, visiting)) {
                throw BusinessException.of(ErrorCode.BPM_SCHEMA_INVALID, "流程模板存在环：" + key);
            }
        }
    }

    private boolean hasCycle(
            String key, Map<String, List<String>> edges, Set<String> visited, Set<String> visiting) {
        if (visiting.contains(key)) {
            return true;
        }
        if (visited.contains(key)) {
            return false;
        }
        visiting.add(key);
        for (String next : edges.getOrDefault(key, List.of())) {
            if (hasCycle(next, edges, visited, visiting)) {
                return true;
            }
        }
        visiting.remove(key);
        visited.add(key);
        return false;
    }

    private BpmSchemaNode parseNode(JsonNode item, int fallbackSeq) {
        String nodeKey = text(item, "nodeKey");
        if (!StringUtils.hasText(nodeKey)) {
            throw BusinessException.of(ErrorCode.BPM_SCHEMA_INVALID, "节点编码 nodeKey 不能为空");
        }
        int seqNo = item.hasNonNull("seqNo") ? item.get("seqNo").asInt() : fallbackSeq;
        if (seqNo <= 0) {
            throw BusinessException.of(ErrorCode.BPM_SCHEMA_INVALID, "节点顺序号必须大于 0");
        }
        BpmSchemaNode node = new BpmSchemaNode();
        node.setNodeKey(nodeKey);
        node.setNodeName(text(item, "nodeName"));
        node.setSeqNo(seqNo);
        node.setNodeType(resolveNodeType(item));
        node.setDueHours(item.hasNonNull("dueHours") ? item.get("dueHours").asInt() : null);
        node.setAssignees(parseAssignees(item));
        node.setNextNodeKey(firstText(item, "nextNodeKey", "next"));
        node.setDefaultNext(firstText(item, "defaultNext", "defaultNextNodeKey"));
        node.setConditions(parseConditions(item));
        node.setAutoApproveIfInitiator(
                item.hasNonNull("autoApproveIfInitiator")
                        && item.get("autoApproveIfInitiator").asBoolean(false));
        node.setAutoApproveConditions(parseAutoConditions(item));

        if (requiresAssignee(node.getNodeType()) && node.getAssignees().isEmpty()) {
            throw BusinessException.of(ErrorCode.BPM_SCHEMA_INVALID, "节点 " + nodeKey + " 缺少审批人配置");
        }
        return node;
    }

    private BpmNodeType resolveNodeType(JsonNode item) {
        String rawType = firstText(item, "nodeType", "BpmNodeType", "NodeType");
        String approveMode = text(item, "approveMode");
        if (!StringUtils.hasText(rawType)) {
            rawType = "APPROVAL";
        }
        String upper = rawType.toUpperCase(Locale.ROOT);
        switch (upper) {
            case "START":
                return BpmNodeType.START;
            case "END":
                return BpmNodeType.END;
            case "CC":
                return BpmNodeType.CC;
            case "CONDITION":
                return BpmNodeType.CONDITION;
            case "AUTO":
                return BpmNodeType.AUTO;
            case "SINGLE":
                return BpmNodeType.SINGLE;
            case "COUNTERSIGN":
                return BpmNodeType.COUNTERSIGN;
            case "OR_SIGN":
                return BpmNodeType.OR_SIGN;
            case "APPROVAL":
                return "ANY".equalsIgnoreCase(approveMode) ? BpmNodeType.OR_SIGN : BpmNodeType.COUNTERSIGN;
            default:
                throw BusinessException.of(ErrorCode.BPM_SCHEMA_INVALID, "不支持的节点类型：" + rawType);
        }
    }

    private List<BpmSchemaCondition> parseConditions(JsonNode item) {
        List<BpmSchemaCondition> result = new ArrayList<>();
        JsonNode array = item.get("conditions");
        if (array == null || !array.isArray()) {
            return result;
        }
        for (JsonNode conditionNode : array) {
            BpmSchemaCondition condition = new BpmSchemaCondition();
            condition.setField(text(conditionNode, "field"));
            condition.setOperator(text(conditionNode, "operator"));
            condition.setValue(text(conditionNode, "value"));
            condition.setTargetNodeKey(firstText(conditionNode, "targetNodeKey", "target"));
            if (!StringUtils.hasText(condition.getField())
                    || !StringUtils.hasText(condition.getOperator())
                    || !StringUtils.hasText(condition.getTargetNodeKey())) {
                throw BusinessException.of(ErrorCode.BPM_SCHEMA_INVALID, "条件配置缺少 field/operator/targetNodeKey");
            }
            result.add(condition);
        }
        return result;
    }

    /** 解析自动通过条件（autoApproveWhen）：不要求 targetNodeKey，其余结构同 conditions。 */
    private List<BpmSchemaCondition> parseAutoConditions(JsonNode item) {
        List<BpmSchemaCondition> result = new ArrayList<>();
        JsonNode array = item.get("autoApproveWhen");
        if (array == null || !array.isArray()) {
            return result;
        }
        for (JsonNode conditionNode : array) {
            BpmSchemaCondition condition = new BpmSchemaCondition();
            condition.setField(text(conditionNode, "field"));
            condition.setOperator(text(conditionNode, "operator"));
            condition.setValue(text(conditionNode, "value"));
            if (!StringUtils.hasText(condition.getField())
                    || !StringUtils.hasText(condition.getOperator())) {
                throw BusinessException.of(
                        ErrorCode.BPM_SCHEMA_INVALID, "自动通过条件缺少 field/operator");
            }
            result.add(condition);
        }
        return result;
    }

    private List<BpmAssigneeSourceConfig> parseAssignees(JsonNode item) {
        List<BpmAssigneeSourceConfig> result = new ArrayList<>();
        JsonNode array = item.get("assignees");
        if (array != null && array.isArray()) {
            for (JsonNode sourceNode : array) {
                result.add(parseAssignee(sourceNode));
            }
        }
        JsonNode single = item.get("assignee");
        if (single != null && single.isObject()) {
            result.add(parseAssignee(single));
        }
        String singleType = text(item, "assigneeType");
        if (StringUtils.hasText(singleType)) {
            BpmAssigneeSourceConfig source = new BpmAssigneeSourceConfig();
            source.setType(parseSourceType(singleType));
            source.setValue(text(item, "assigneeValue"));
            result.add(source);
        }
        return result;
    }

    private BpmAssigneeSourceConfig parseAssignee(JsonNode node) {
        String rawType = text(node, "type");
        if (!StringUtils.hasText(rawType)) {
            throw BusinessException.of(ErrorCode.BPM_SCHEMA_INVALID, "审批人来源缺少 type");
        }
        BpmAssigneeSourceConfig source = new BpmAssigneeSourceConfig();
        source.setType(parseSourceType(rawType));
        source.setValue(text(node, "value"));
        if (source.getType() == BpmAssigneeSource.USER && !StringUtils.hasText(source.getValue())) {
            throw BusinessException.of(ErrorCode.BPM_SCHEMA_INVALID, "指定用户来源缺少用户ID");
        }
        if (source.getType() == BpmAssigneeSource.ROLE && !StringUtils.hasText(source.getValue())) {
            throw BusinessException.of(ErrorCode.BPM_SCHEMA_INVALID, "指定角色来源缺少角色编码");
        }
        return source;
    }

    private BpmAssigneeSource parseSourceType(String raw) {
        try {
            return BpmAssigneeSource.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw BusinessException.of(ErrorCode.BPM_SCHEMA_INVALID, "不支持的审批人来源：" + raw);
        }
    }

    private boolean requiresAssignee(BpmNodeType type) {
        return type == BpmNodeType.SINGLE
                || type == BpmNodeType.COUNTERSIGN
                || type == BpmNodeType.OR_SIGN
                || type == BpmNodeType.CC;
    }

    private String text(JsonNode item, String field) {
        JsonNode value = item.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private String firstText(JsonNode item, String... fields) {
        for (String field : fields) {
            String value = text(item, field);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }
}
