package com.ceams.common.pdf;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PDF 盖章请求参数。
 *
 * <p>坐标 {@code x/y} 为 PDF 页面左下角坐标系下的位置（单位：PDF point，1/72 英寸），
 * 传 {@code null} 时默认将印章居中放置于目标页中心。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StampRequest {

    /** 印章图像字节（PNG/JPEG）。 */
    private byte[] sealImage;

    /** 印章左下角 X 坐标，可空（默认页面水平居中）。 */
    private Integer x;

    /** 印章左下角 Y 坐标，可空（默认页面垂直居中）。 */
    private Integer y;

    /** 目标页码（0 起始索引）。 */
    private int pageNumber;

    /** 缩放比例（>0，小于等于 0 时按 1.0 处理）。 */
    private float scale;
}
