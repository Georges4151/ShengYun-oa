package com.ceams.module.doc.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** 目录路径工具单元测试：路径生成、子树判定与自身命中。 */
class DocPathUtilTest {

    @Test
    void rootPathShouldWrapIdWithSlashes() {
        assertThat(DocPathUtil.rootPath(12L)).isEqualTo("/12/");
        assertThatThrownBy(() -> DocPathUtil.rootPath(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void childPathShouldAppendIdToParentPath() {
        assertThat(DocPathUtil.childPath("/1/", 2L)).isEqualTo("/1/2/");
        // 父路径为空表示库根
        assertThat(DocPathUtil.childPath(null, 7L)).isEqualTo("/7/");
        // 缺少尾斜杠时自动补全
        assertThat(DocPathUtil.childPath("/1", 2L)).isEqualTo("/1/2/");
    }

    @Test
    void isDescendantShouldMatchOnlyRealSubtree() {
        assertThat(DocPathUtil.isDescendant("/1/", "/1/2/")).isTrue();
        assertThat(DocPathUtil.isDescendant("/1/2/", "/1/2/3/4/")).isTrue();
        // 严格祖先：相同路径不算后代
        assertThat(DocPathUtil.isDescendant("/1/", "/1/")).isFalse();
        // 前缀相似但不是子树
        assertThat(DocPathUtil.isDescendant("/1/", "/11/")).isFalse();
        assertThat(DocPathUtil.isDescendant(null, "/1/")).isFalse();
    }

    @Test
    void isSelfOrDescendantShouldHitSelf() {
        assertThat(DocPathUtil.isSelfOrDescendant(11L, "/11/")).isTrue();
        assertThat(DocPathUtil.isSelfOrDescendant(11L, "/11/12/")).isTrue();
        assertThat(DocPathUtil.isSelfOrDescendant(11L, "/11/12/13/")).isTrue();
        assertThat(DocPathUtil.isSelfOrDescendant(11L, "/12/")).isFalse();
        assertThat(DocPathUtil.isSelfOrDescendant(null, "/11/")).isFalse();
    }
}
