package com.ceams.common.pdf;

/**
 * PDF 盖章服务：将印章图像叠加到 PDF 指定页。
 *
 * <p>该接口是跨域复用的服务端能力，供 seal 域电子盖章等场景调用。</p>
 */
public interface PdfStampService {

    /**
     * 对源 PDF 指定页叠加印章，返回新 PDF 字节。
     *
     * @param sourcePdf 源 PDF 字节
     * @param request   盖章参数（印章图像、坐标、页码、缩放比例）
     * @return 盖章后的 PDF 字节
     */
    byte[] stamp(byte[] sourcePdf, StampRequest request);
}
