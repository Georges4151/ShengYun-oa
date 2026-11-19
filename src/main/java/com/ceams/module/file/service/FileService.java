package com.ceams.module.file.service;

import com.ceams.module.file.vo.FileStorageInfoVO;
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

    /**
     * 读取已上传文件的字节，供票据识别等内部服务使用（无需自行拼接落盘目录）。
     *
     * @param name 服务端文件名（含后缀）
     * @return 文件字节
     */
    byte[] readBytes(String name);

    /** 删除未被业务数据引用的上传文件；文件不存在时静默成功。 */
    void delete(String name);

    /**
     * 获取上传目录的只读存储信息（路径、占用统计、所在分区剩余空间）。
     *
     * <p>纯只读快照，不创建目录、不修改任何文件；目录不存在或部分文件不可读时降级返回已有统计， 不抛出异常。
     *
     * @return 存储信息视图对象
     */
    FileStorageInfoVO storageInfo();
}
