package com.ceams.module.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.common.security.SecurityUtils;
import com.ceams.module.ai.dto.AiFeedbackRequest;
import com.ceams.module.ai.entity.AiConversation;
import com.ceams.module.ai.entity.AiFeedback;
import com.ceams.module.ai.entity.AiMessage;
import com.ceams.module.ai.mapper.AiConversationMapper;
import com.ceams.module.ai.mapper.AiFeedbackMapper;
import com.ceams.module.ai.mapper.AiMessageMapper;
import com.ceams.module.ai.service.AiConversationService;
import com.ceams.module.ai.vo.AiConversationVO;
import com.ceams.module.ai.vo.AiMessageVO;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** AI 会话查询与消息评价实现：所有查询强制限定当前用户。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiConversationServiceImpl implements AiConversationService {

    /** 与其他模块分页上限保持一致（MyBatis-Plus 分页插件同样限制 100）。 */
    private static final long MAX_PAGE_SIZE = 100L;

    private final AiConversationMapper conversationMapper;
    private final AiMessageMapper messageMapper;
    private final AiFeedbackMapper feedbackMapper;

    @Override
    public PageResult<AiConversationVO> conversations(long page, long size) {
        Long userId = requireUserId();
        Page<AiConversation> request = resolvePage(page, size);
        LambdaQueryWrapper<AiConversation> wrapper =
                new LambdaQueryWrapper<AiConversation>()
                        .eq(AiConversation::getUserId, userId)
                        .orderByDesc(AiConversation::getUpdateTime)
                        .orderByDesc(AiConversation::getId);
        IPage<AiConversation> result = conversationMapper.selectPage(request, wrapper);
        List<AiConversationVO> records = new ArrayList<>();
        if (result != null && result.getRecords() != null) {
            for (AiConversation conversation : result.getRecords()) {
                records.add(toConversationVO(conversation));
            }
        }
        return new PageResult<>(
                records,
                result == null ? 0L : result.getTotal(),
                result == null ? request.getCurrent() : result.getCurrent(),
                result == null ? request.getSize() : result.getSize());
    }

    @Override
    public PageResult<AiMessageVO> messages(Long conversationId, long page, long size) {
        requireOwnedConversation(conversationId);
        Page<AiMessage> request = resolvePage(page, size);
        LambdaQueryWrapper<AiMessage> wrapper =
                new LambdaQueryWrapper<AiMessage>()
                        .eq(AiMessage::getConversationId, conversationId)
                        .orderByAsc(AiMessage::getCreateTime)
                        .orderByAsc(AiMessage::getId);
        IPage<AiMessage> result = messageMapper.selectPage(request, wrapper);
        List<AiMessageVO> records = new ArrayList<>();
        if (result != null && result.getRecords() != null) {
            for (AiMessage message : result.getRecords()) {
                records.add(toMessageVO(message));
            }
        }
        return new PageResult<>(
                records,
                result == null ? 0L : result.getTotal(),
                result == null ? request.getCurrent() : result.getCurrent(),
                result == null ? request.getSize() : result.getSize());
    }

    @Override
    public void feedback(AiFeedbackRequest request) {
        Long userId = requireUserId();
        int score = request.getScore() == null ? 0 : request.getScore();
        if (score != 1 && score != -1) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "评分只能为 1 或 -1");
        }
        Long messageId = parseId(request.getMessageId(), "messageId");
        AiMessage message = messageMapper.selectById(messageId);
        if (message == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "消息不存在");
        }
        requireOwnedConversation(message.getConversationId());

        AiFeedback feedback = new AiFeedback();
        feedback.setMessageId(messageId);
        feedback.setUserId(userId);
        feedback.setScore(score);
        feedback.setRemark(request.getRemark());
        feedback.setCreateBy(userId);
        feedback.setUpdateBy(userId);
        feedbackMapper.insert(feedback);
    }

    /** 会话必须存在且属于当前用户，否则统一返回 404 语义。 */
    private AiConversation requireOwnedConversation(Long conversationId) {
        if (conversationId == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "会话不存在或无权访问");
        }
        Long userId = requireUserId();
        AiConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null
                || conversation.getUserId() == null
                || !conversation.getUserId().equals(userId)) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "会话不存在或无权访问");
        }
        return conversation;
    }

    private AiConversationVO toConversationVO(AiConversation conversation) {
        AiConversationVO vo = new AiConversationVO();
        vo.setId(conversation.getId());
        vo.setTitle(conversation.getTitle());
        vo.setCreateTime(conversation.getCreateTime());
        vo.setUpdateTime(conversation.getUpdateTime());
        return vo;
    }

    private AiMessageVO toMessageVO(AiMessage message) {
        AiMessageVO vo = new AiMessageVO();
        vo.setId(message.getId());
        vo.setRole(message.getRole());
        vo.setContent(message.getContent());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }

    /** 统一分页校验：页码/条数越界抛 VALID_PAGE_BOUNDS，条数上限 100。 */
    private static <T> Page<T> resolvePage(long page, long size) {
        if (page < 1) {
            throw BusinessException.of(ErrorCode.VALID_PAGE_BOUNDS, "页码必须不小于 1");
        }
        if (size < 1) {
            throw BusinessException.of(ErrorCode.VALID_PAGE_BOUNDS, "每页条数必须不小于 1");
        }
        if (size > MAX_PAGE_SIZE) {
            throw BusinessException.of(ErrorCode.VALID_PAGE_BOUNDS, "每页条数不能超过 " + MAX_PAGE_SIZE);
        }
        return new Page<>(page, size);
    }

    private static Long requireUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw BusinessException.of(ErrorCode.AUTH_TOKEN_INVALID, "未登录或登录上下文已失效");
        }
        return userId;
    }

    private static Long parseId(String rawId, String label) {
        if (!StringUtils.hasText(rawId)) {
            throw BusinessException.of(ErrorCode.VALID_REQUIRED_FIELD, label + "不能为空");
        }
        try {
            return Long.parseLong(rawId.trim());
        } catch (NumberFormatException e) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, label + "非法：" + rawId);
        }
    }
}
