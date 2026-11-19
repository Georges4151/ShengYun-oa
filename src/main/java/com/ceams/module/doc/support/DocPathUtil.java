package com.ceams.module.doc.support;

/**
 * 文档目录路径工具：路径格式统一为 {@code /id/id/}（含首尾斜杠）。
 *
 * <p>纯静态方法，无 Spring 依赖，便于单元测试。路径语义：
 *
 * <ul>
 *   <li>{@code /1/}：根目录 1；
 *   <li>{@code /1/2/3/}：1 为根，2 为 1 的子目录，3 为 2 的子目录。
 * </ul>
 */
public final class DocPathUtil {

    private DocPathUtil() {}

    /** 根目录路径：{@code /id/}。 */
    public static String rootPath(Long id) {
        requireId(id);
        return "/" + id + "/";
    }

    /** 子目录路径：{@code parentPath + id + "/"}，父路径为空表示库根。 */
    public static String childPath(String parentPath, Long id) {
        requireId(id);
        return normalize(parentPath) + id + "/";
    }

    /**
     * 判断 {@code ancestorPath} 是否为 {@code targetPath} 的严格祖先。
     *
     * <p>相同路径返回 false；空值返回 false；{@code /1/} 不是 {@code /11/} 的祖先（按斜杠分段匹配）。
     */
    public static boolean isDescendant(String ancestorPath, String targetPath) {
        if (isBlank(ancestorPath) || isBlank(targetPath)) {
            return false;
        }
        String ancestor = normalize(ancestorPath);
        String target = normalize(targetPath);
        return !ancestor.equals(target) && target.startsWith(ancestor);
    }

    /** 判断 {@code targetPath} 是否为 {@code dirId} 自身或其子树路径。 */
    public static boolean isSelfOrDescendant(Long dirId, String targetPath) {
        if (dirId == null || isBlank(targetPath)) {
            return false;
        }
        String selfPath = rootPath(dirId);
        return normalize(targetPath).startsWith(selfPath);
    }

    /**
     * 路径前缀替换：把以 {@code oldPrefix} 开头的路径改写为 {@code newPrefix} 开头，用于移动后刷新子树。
     *
     * <p>不以 {@code oldPrefix} 开头时原样返回，空值原样返回。
     */
    public static String rebase(String path, String oldPrefix, String newPrefix) {
        if (isBlank(path) || isBlank(oldPrefix)) {
            return path;
        }
        if (!path.startsWith(oldPrefix)) {
            return path;
        }
        return (isBlank(newPrefix) ? "" : newPrefix) + path.substring(oldPrefix.length());
    }

    /** 路径归一化：空值返回根前缀 {@code "/"}，缺失尾部斜杠时补全。 */
    private static String normalize(String path) {
        if (isBlank(path)) {
            return "/";
        }
        return path.endsWith("/") ? path : path + "/";
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static void requireId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("目录 ID 不能为空");
        }
    }
}
