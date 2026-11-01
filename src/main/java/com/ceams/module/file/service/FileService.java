package com.ceams.module.file.service;

import com.ceams.module.file.vo.FileVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 轻量文件上传/下载服务（file 模块，只读应用层定位，本地落盘、不建表、不开写事务）。
 */
public interface FileService {

    /**
     * 上传文件并落盘本地目录，返回带相对访问路径的结果。
     *
     * @param file 上传的文件
     * @return 上传结果视图对象
     */
    FileVO upload(MultipartFile file);

    /**
     * 读取已落盘文件字节。
     *
     * @param name 服务端文件名（含后缀，已通过正则校验）
     * @return 文件字节
     */
    byte[] load(String name);
}