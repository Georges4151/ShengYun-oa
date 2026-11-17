package com.ceams.module.bpm.support;

import com.ceams.module.bpm.entity.BpmCcRecord;
import com.ceams.module.bpm.entity.BpmInstance;
import com.ceams.module.bpm.entity.BpmNode;
import com.ceams.module.bpm.entity.BpmTask;
import com.ceams.module.bpm.enums.BpmNodeStatus;
import com.ceams.module.bpm.enums.BpmTaskStatus;
import com.ceams.module.bpm.vo.CcRecordVO;
import com.ceams.module.bpm.vo.InstanceVO;
import com.ceams.module.bpm.vo.NodeVO;
import com.ceams.module.bpm.vo.TaskVO;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** BPM 视图装配器：实体到 VO 的统一转换，避免各服务重复拼装。 */
@Component
public class BpmViewAssembler {

    /** 实例概要视图（含当前活动节点与处理人）。 */
    public InstanceVO toInstanceVO(
            BpmInstance instance, List<BpmNode> nodes, List<BpmTask> tasks) {
        InstanceVO vo = new InstanceVO();
        vo.setId(instance.getId());
        vo.setTemplateVersionId(instance.getTemplateVersionId());
        vo.setBizType(instance.getBizType());
        vo.setBizId(instance.getBizId());
        vo.setInitiatorId(instance.getInitiatorId());
        vo.setStatus(instance.getStatus());
        vo.setStartedAt(instance.getStartedAt());
        vo.setEndedAt(instance.getEndedAt());
        vo.setVersion(instance.getVersion());
        vo.setCreateTime(instance.getCreateTime());

        BpmNode activeNode =
                nodes.stream()
                        .filter(n -> n.getStatus() == BpmNodeStatus.ACTIVE)
                        .min(Comparator.comparing(BpmNode::getSeqNo))
                        .orElse(null);
        if (activeNode != null) {
            vo.setCurrentNodeKey(activeNode.getNodeKey());
            vo.setCurrentNodeStatus(activeNode.getStatus().name());
            vo.setCurrentAssigneeIds(
                    tasks.stream()
                            .filter(t -> Objects.equals(t.getNodeId(), activeNode.getId()))
                            .filter(t -> t.getStatus() == BpmTaskStatus.PENDING)
                            .map(BpmTask::getAssigneeId)
                            .filter(Objects::nonNull)
                            .distinct()
                            .toList());
        }
        return vo;
    }

    /** 节点视图列表（按 seqNo 升序，附带各自任务）。 */
    public List<NodeVO> toNodeVOs(List<BpmNode> nodes, List<BpmTask> tasks) {
        Map<Long, List<BpmTask>> taskMap =
                tasks.stream().collect(Collectors.groupingBy(BpmTask::getNodeId));
        return nodes.stream()
                .sorted(Comparator.comparing(BpmNode::getSeqNo))
                .map(
                        node -> {
                            NodeVO vo = new NodeVO();
                            vo.setId(node.getId());
                            vo.setNodeKey(node.getNodeKey());
                            vo.setNodeType(node.getNodeType());
                            vo.setSeqNo(node.getSeqNo());
                            vo.setStatus(node.getStatus());
                            vo.setArrivedAt(node.getArrivedAt());
                            vo.setCompletedAt(node.getCompletedAt());
                            vo.setTasks(
                                    taskMap.getOrDefault(node.getId(), List.of()).stream()
                                            .map(this::toTaskVO)
                                            .toList());
                            return vo;
                        })
                .toList();
    }

    /** 任务视图（不含实例冗余信息，用于节点详情）。 */
    public TaskVO toTaskVO(BpmTask task) {
        TaskVO vo = new TaskVO();
        fillTask(vo, task);
        return vo;
    }

    /** 待办视图（附带实例与节点冗余信息）。 */
    public TaskVO toTaskVO(
            BpmTask task,
            Map<Long, BpmInstance> instanceMap,
            Map<Long, BpmNode> nodeMap) {
        TaskVO vo = toTaskVO(task);
        BpmInstance instance = instanceMap.get(task.getInstanceId());
        if (instance != null) {
            vo.setBizType(instance.getBizType());
            vo.setBizId(instance.getBizId());
            vo.setInstanceStatus(instance.getStatus());
            vo.setInitiatorId(instance.getInitiatorId());
        }
        BpmNode node = nodeMap.get(task.getNodeId());
        if (node != null) {
            vo.setNodeKey(node.getNodeKey());
        }
        return vo;
    }

    /** 抄送视图。 */
    public CcRecordVO toCcRecordVO(
            BpmCcRecord record, Map<Long, BpmInstance> instanceMap, Map<Long, BpmNode> nodeMap) {
        CcRecordVO vo = new CcRecordVO();
        vo.setId(record.getId());
        vo.setInstanceId(record.getInstanceId());
        vo.setNodeId(record.getNodeId());
        vo.setReceiverId(record.getReceiverId());
        vo.setReadStatus(record.getReadStatus());
        vo.setCreateTime(record.getCreateTime());
        BpmInstance instance = instanceMap.get(record.getInstanceId());
        if (instance != null) {
            vo.setBizType(instance.getBizType());
            vo.setBizId(instance.getBizId());
        }
        BpmNode node = nodeMap.get(record.getNodeId());
        if (node != null) {
            vo.setNodeKey(node.getNodeKey());
        }
        return vo;
    }

    /** 批量转 Map，便于装配时避免 N+1。 */
    public <T> Map<Long, T> index(List<T> list, Function<T, Long> idGetter) {
        return list.stream()
                .filter(item -> idGetter.apply(item) != null)
                .collect(Collectors.toMap(idGetter, item -> item, (a, b) -> a));
    }

    private void fillTask(TaskVO vo, BpmTask task) {
        vo.setId(task.getId());
        vo.setInstanceId(task.getInstanceId());
        vo.setNodeId(task.getNodeId());
        vo.setAssigneeId(task.getAssigneeId());
        vo.setOwnerAssigneeId(task.getOwnerAssigneeId());
        vo.setSourceType(task.getSourceType());
        vo.setTaskVersion(task.getVersion());
        vo.setTaskType(task.getTaskType());
        vo.setStatus(task.getStatus());
        vo.setDueAt(task.getDueAt());
        vo.setOverdue(
                task.getDueAt() != null
                        && task.getStatus() == BpmTaskStatus.PENDING
                        && task.getDueAt().isBefore(LocalDateTime.now()));
        vo.setCreateTime(task.getCreateTime());
        vo.setUpdateTime(task.getUpdateTime());
    }
}
