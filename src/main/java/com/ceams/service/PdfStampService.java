package com.ceams.service;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.FileOutputStream;
import java.io.IOException;

public interface PdfStampService {
        /**
         * 在PDF文件上盖章并导出
         * 
         * @param inputPath       输入PDF文件路径
         * @param outputPath      输出PDF文件路径
         * @param sealImageBase64 电子章图片的Base64编码
         * @param x               横坐标
         * @param y               纵坐标
         * @param width           图片宽度
         * @param height          图片高度
         */
        void stampPdf(String inputPath, String outputPath, String sealImageBase64, float x, float y, float width,
                        float height)
                        throws IOException, DocumentException;

        /**
         * 在PDF文件上盖章并导出（使用坐标位置）
         * 
         * @param inputPath       输入PDF文件路径
         * @param outputPath      输出PDF文件路径
         * @param sealImageBase64 电子章图片的Base64编码
         * @param page            页码
         * @param position        位置描述（top_left, top_right, bottom_left, bottom_right,
         *                        center）
         */
        void stampPdfWithPosition(String inputPath, String outputPath, String sealImageBase64, int page,
                        String position)
                        throws IOException, DocumentException;

        /**
         * 在PDF文件的指定坐标位置盖章
         * 
         * @param inputPath       输入PDF文件路径
         * @param outputPath      输出PDF文件路径
         * @param sealImageBase64 电子章图片的Base64编码
         * @param page            页码（从1开始）
         * @param x               X坐标（从左下角开始）
         * @param y               Y坐标（从左下角开始）
         * @param size            印章大小
         */
        void stampPdfAtPosition(String inputPath, String outputPath, String sealImageBase64, int page,
                        float x, float y, float size)
                        throws IOException, DocumentException;
}
