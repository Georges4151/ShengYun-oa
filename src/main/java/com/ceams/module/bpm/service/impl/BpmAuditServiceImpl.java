package com.ceams.module.bpm.service.impl;

import com.ceams.module.bpm.entity.BizAuditSnapshot;
import com.ceams.module.bpm.enums.AuditAction;
import com.ceams.module.bpm.mapper.BizAuditSnapshotMapper;
import com.ceams.module.bpm.service.BpmAuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** BPM 审计快照服务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BpmAuditServiceImpl implements BpmAuditService {

    private final BizAuditSnapshotMapper bizAuditSnapshotMapper;
    private final ObjectMapper objectMapper;

    @Override
    public String record(
            String targetType,
            Long targetId,
            AuditAction action,
            Object before,
            Object after,
            String maskedFields,
            Long operatorId) {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        BizAuditSnapshot snapshot = new BizAuditSnapshot();
        snapshot.setTargetType(targetType);
        snapshot.setTargetId(targetId);
        snapshot.setAction(action.name());
        snapshot.setBeforeJson(toJson(before));
        snapshot.setAfterJson(toJson(after));
        snapshot.setMaskedFields(maskedFields);
        snapshot.setOperatorId(operatorId);
        snapshot.setTraceId(traceId);
        bizAuditSnapshotMapper.insert(snapshot);
        log.info(
                "BPM 审计写入：action={}，targetType={}，targetId={}，operator={}，traceId={}",
                action,
                targetType,
                targetId,
                operatorId,
                traceId);
        return traceId;
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("BPM 审计快照序列化失败：{}", e.getMessage());
            return null;
        }
    }
}
