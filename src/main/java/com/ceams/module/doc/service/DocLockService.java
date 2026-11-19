package com.ceams.module.doc.service;

import com.ceams.module.doc.vo.DocLockVO;

/** 文档编辑锁服务（Redis 为准，数据库 status / lock_user_id 仅作展示冗余）。 */
public interface DocLockService {

    /** 加锁（同用户重复加锁幂等成功并续期；他人持有则 409）。 */
    void lock(Long fileId);

    /** 续期（仅持有人可续，否则 409）。 */
    void renew(Long fileId);

    /** 释放（仅持有人可释放；Redis 无锁时按幂等成功处理）。 */
    void unlock(Long fileId);

    /** 强制释放（忽略持有人，供 OnlyOffice 保存回调使用；幂等，Redis 无 key 也不报错）。 */
    void forceUnlock(Long fileId);

    /** 查询锁状态（Redis 无 key 但数据库仍 LOCKED 时回写校准）。 */
    DocLockVO status(Long fileId);
}
