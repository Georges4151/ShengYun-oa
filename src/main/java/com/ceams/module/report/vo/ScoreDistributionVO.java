package com.ceams.module.report.vo;

import lombok.Data;

/** 绩效分数分布：A ≥ 90、B 80~89.99、C 70~79.99、D &lt; 70，仅统计已定稿且分数非空的考评单。 */
@Data
public class ScoreDistributionVO {

    /** A 档人数（≥ 90）。 */
    private long a;

    /** B 档人数（80 ~ 89.99）。 */
    private long b;

    /** C 档人数（70 ~ 79.99）。 */
    private long c;

    /** D 档人数（&lt; 70）。 */
    private long d;
}
