package com.ceams.module.bpm.support;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** 解析后的流程定义（一次模板版本对应的节点集合）。 */
@Data
public class BpmSchema {

    /** 节点定义（按 seqNo 升序）。 */
    private List<BpmSchemaNode> nodes = new ArrayList<>();

    /** 按节点编码查找定义（找不到返回 null）。 */
    public BpmSchemaNode findByKey(String nodeKey) {
        return nodes.stream().filter(n -> n.getNodeKey().equals(nodeKey)).findFirst().orElse(null);
    }

    /** 按顺序号查找定义（找不到返回 null）。 */
    public BpmSchemaNode findBySeq(Integer seqNo) {
        return nodes.stream().filter(n -> n.getSeqNo().equals(seqNo)).findFirst().orElse(null);
    }
}
