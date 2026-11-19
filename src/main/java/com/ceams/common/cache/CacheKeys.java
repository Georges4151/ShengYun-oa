package com.ceams.common.cache;

/**
 * 业务缓存 key 统一管理。
 *
 * <p>全部 key 以 {@code ceams:cache:} 开头，便于按前缀区分业务缓存与其他 Redis 数据 （nonce、文档锁、企业微信
 * token 等）。
 */
public final class CacheKeys {

    /** 业务缓存统一前缀。 */
    public static final String PREFIX = "ceams:cache:";

    /** 权限树（全局字典数据，与用户无关，可安全共享）。 */
    public static final String PERMISSION_TREE = PREFIX + "iam:permission:tree";

    /** 分类树 key 前缀，按业务类型区分。 */
    public static final String CATEGORY_TREE_PREFIX = PREFIX + "masterdata:category:tree:";

    private CacheKeys() {}

    /**
     * 构造指定类型的分类树缓存 key。
     *
     * @param typeName 分类业务类型名称，如 {@code EXPENSE}
     * @return 形如 {@code ceams:cache:masterdata:category:tree:EXPENSE} 的 key
     */
    public static String categoryTree(String typeName) {
        return CATEGORY_TREE_PREFIX + typeName;
    }
}
