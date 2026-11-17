package com.ceams.module.doc.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.security.SecurityUtils;
import com.ceams.module.bpm.support.BpmSecurityGuard;
import com.ceams.module.doc.config.DocProperties;
import com.ceams.module.doc.entity.DocDirectory;
import com.ceams.module.doc.entity.DocFile;
import com.ceams.module.doc.enums.DocAction;
import com.ceams.module.doc.enums.DocFileStatus;
import com.ceams.module.doc.mapper.DocDirectoryMapper;
import com.ceams.module.doc.mapper.DocFileMapper;
import com.ceams.module.doc.service.DocLockService;
import com.ceams.module.doc.support.DocAccessResolver;
import com.ceams.module.doc.vo.DocLockVO;
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.mapper.UserMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 文档编辑锁服务实现。
 *
 * <p>口径：
 *
 * <ul>
 *   <li>Redis {@code doc:lock:{fileId}}（值 = userId，TTL {@code lock-ttl-minutes}）为唯一事实来源，
 *       数据库 {@code status / lock_user_id / lock_time} 仅作列表展示冗余。
 *   <li>他人持锁时加锁 / 续期 / 释放统一返回 409「文件正在被他人编辑」。
 *   <li>同一用户重复加锁按幂等成功处理并顺延续期（前端重复点击不会报错）。
 *   <li>Redis key 已过期但数据库仍为 LOCKED 时，查询状态会回写校准。
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocLockServiceImpl implements DocLockService {

    /** 编辑权限码（V39 种子）。 */
    private static final String PERMISSION_FILE_EDIT = "doc:file:edit";

    /** 锁 key 前缀。 */
    private static final String LOCK_KEY_PREFIX = "doc:lock:";

    /** 他人持锁提示。 */
    private static final String MESSAGE_LOCKED_BY_OTHER = "文件正在被他人编辑";

    private final StringRedisTemplate stringRedisTemplate;
    private final DocFileMapper fileMapper;
    private final DocDirectoryMapper dirMapper;
    private final DocAccessResolver accessResolver;
    private final BpmSecurityGuard securityGuard;
    private final UserMapper userMapper;
    private final DocProperties docProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lock(Long fileId) {
        Long userId = requireCurrentUserId();
        DocFile file = requireFile(fileId);
        DocDirectory dir = requireDir(file.getDirId());
        requireEdit(dir, file);

        String key = lockKey(fileId);
        String owner = String.valueOf(userId);
        Duration ttl = lockTtl();
        Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(key, owner, ttl);
        if (!Boolean.TRUE.equals(acquired)) {
            String holder = stringRedisTemplate.opsForValue().get(key);
            if (!owner.equals(holder)) {
                throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, MESSAGE_LOCKED_BY_OTHER);
            }
            // 同一用户重复加锁：幂等成功并顺延续期
            stringRedisTemplate.expire(key, ttl);
        }
        syncLocked(fileId, userId);
        log.info("文档文件 {} 已加锁：owner={}", fileId, userId);
    }

    @Override
    public void renew(Long fileId) {
        Long userId = requireCurrentUserId();
        DocFile file = requireFile(fileId);
        DocDirectory dir = requireDir(file.getDirId());
        requireEdit(dir, file);

        String key = lockKey(fileId);
        String holder = stringRedisTemplate.opsForValue().get(key);
        if (StringUtils.isBlank(holder)) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "文件锁已过期，请重新加锁");
        }
        if (!String.valueOf(userId).equals(holder)) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, MESSAGE_LOCKED_BY_OTHER);
        }
        stringRedisTemplate.expire(key, lockTtl());
        syncLocked(fileId, userId);
        log.info("文档文件 {} 锁已续期：owner={}", fileId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlock(Long fileId) {
        Long userId = requireCurrentUserId();
        DocFile file = requireFile(fileId);
        DocDirectory dir = requireDir(file.getDirId());
        requireEdit(dir, file);

        String key = lockKey(fileId);
        String holder = stringRedisTemplate.opsForValue().get(key);
        if (StringUtils.isNotBlank(holder) && !String.valueOf(userId).equals(holder)) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, MESSAGE_LOCKED_BY_OTHER);
        }
        stringRedisTemplate.delete(key);
        clearLock(fileId);
        log.info("文档文件 {} 锁已释放：operator={}", fileId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void forceUnlock(Long fileId) {
        if (fileId == null) {
            return;
        }
        // 幂等：Redis key 不存在时 delete 返回 false，不抛异常
        stringRedisTemplate.delete(lockKey(fileId));
        // lock_user_id / lock_time 用 SQL NULL 置空，避免 set(field, null) 写法
        UpdateWrapper<DocFile> wrapper =
                new UpdateWrapper<DocFile>()
                        .eq("id", fileId)
                        .set("status", DocFileStatus.NORMAL.name())
                        .setSql("lock_user_id = NULL, lock_time = NULL");
        fileMapper.update(null, wrapper);
        log.info("文档文件 {} 编辑锁已强制释放", fileId);
    }

    @Override
    public DocLockVO status(Long fileId) {
        DocFile file = requireFile(fileId);
        DocDirectory dir = requireDir(file.getDirId());
        accessResolver.require(DocAction.PREVIEW, dir, file);

        String holder = stringRedisTemplate.opsForValue().get(lockKey(fileId));
        DocLockVO vo = new DocLockVO();
        if (StringUtils.isBlank(holder)) {
            // Redis 锁已过期：数据库仍标记 LOCKED 时回写校准
            if (file.getStatus() == DocFileStatus.LOCKED || file.getLockUserId() != null) {
                clearLock(fileId);
                log.info("编辑锁已在 Redis 过期，回写数据库为未锁：fileId={}", fileId);
            }
            vo.setLocked(false);
            return vo;
        }
        Long ownerId = parseLong(holder);
        vo.setLocked(true);
        vo.setOwnerId(ownerId);
        if (ownerId != null) {
            SysUser owner = userMapper.selectById(ownerId);
            vo.setOwnerName(owner == null ? null : owner.getName());
        }
        return vo;
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    private Long requireCurrentUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "未登录不可操作文档编辑锁");
        }
        return userId;
    }

    private DocFile requireFile(Long fileId) {
        DocFile file = fileId == null ? null : fileMapper.selectById(fileId);
        if (file == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "文件不存在");
        }
        return file;
    }

    private DocDirectory requireDir(Long dirId) {
        DocDirectory dir = dirId == null ? null : dirMapper.selectById(dirId);
        if (dir == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "目录不存在");
        }
        return dir;
    }

    /** 编辑权：命中 {@code doc:file:edit} 直接放行（管理员由守卫放行），否则按数据权限校验。 */
    private void requireEdit(DocDirectory dir, DocFile file) {
        if (securityGuard.hasPermission(PERMISSION_FILE_EDIT)) {
            return;
        }
        accessResolver.require(DocAction.EDIT, dir, file);
    }

    private String lockKey(Long fileId) {
        return LOCK_KEY_PREFIX + fileId;
    }

    private Duration lockTtl() {
        return Duration.ofMinutes(Math.max(1, docProperties.getLockTtlMinutes()));
    }

    /** 同步数据库锁标记（status=LOCKED + lock_user_id + lock_time）。 */
    private void syncLocked(Long fileId, Long userId) {
        UpdateWrapper<DocFile> wrapper =
                new UpdateWrapper<DocFile>()
                        .eq("id", fileId)
                        .set("status", DocFileStatus.LOCKED.name())
                        .set("lock_user_id", userId)
                        .set("lock_time", LocalDateTime.now());
        fileMapper.update(null, wrapper);
    }

    /** 清除数据库锁标记（显式 set 空值，避免 updateById 忽略 null 字段）。 */
    private void clearLock(Long fileId) {
        UpdateWrapper<DocFile> wrapper =
                new UpdateWrapper<DocFile>()
                        .eq("id", fileId)
                        .set("status", DocFileStatus.NORMAL.name())
                        .set("lock_user_id", null)
                        .set("lock_time", null);
        fileMapper.update(null, wrapper);
    }

    private Long parseLong(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            log.warn("编辑锁持有人 ID 非法：{}", value);
            return null;
        }
    }
}
