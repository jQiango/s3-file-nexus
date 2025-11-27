package com.all.in.one.agent.storage.service;

import com.all.in.one.agent.storage.config.StorageConfigProperties;
import com.all.in.one.agent.storage.dto.FileListDTO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 存储服务接口
 */
public interface StorageService {

    /**
     * 获取所有后端配置列表
     */
    Map<String, StorageConfigProperties.Backend> getAllBackends();

    /**
     * 获取指定后端配置
     */
    StorageConfigProperties.Backend getBackend(String backendName);

    /**
     * 获取默认后端配置
     */
    StorageConfigProperties.Backend getDefaultBackend();

    /**
     * 获取默认后端的key（配置文件中的key，不是显示名称）
     */
    String getDefaultBackendKey();

    /**
     * 测试存储配置连接
     */
    boolean testConnection(String backendName);

    /**
     * 上传文件
     */
    Map<String, Object> uploadFile(MultipartFile file, String backendName, String bucketName, String objectKey);

    /**
     * 下载文件
     */
    void downloadFile(String backendName, String bucketName, String objectKey, HttpServletResponse response);

    /**
     * 获取文件列表
     */
    Map<String, Object> listFiles(FileListDTO listDTO);

    /**
     * 删除文件
     */
    void deleteFile(String backendName, String bucketName, String objectKey);

    /**
     * 预览文件（流式输出，Content-Disposition=inline）
     */
    void previewFile(String backendName, String bucketName, String objectKey, HttpServletResponse response);

    /**
     * 获取预签名URL（用于临时访问）
     */
    String getPresignedUrl(String backendName, String bucketName, String objectKey, int expirationSeconds);

    /**
     * 创建文件夹
     */
    void createFolder(String backendName, String bucketName, String folderPath);

    /**
     * 获取存储桶列表
     */
    List<String> listBuckets(String backendName);

    /**
     * 批量删除文件
     */
    void batchDeleteFiles(String backendName, String bucketName, List<String> objectKeys);

    /**
     * 重命名文件或文件夹
     */
    void renameFile(String backendName, String bucketName, String oldKey, String newKey);

    /**
     * 计算文件夹大小（递归计算所有子文件）
     */
    long calculateFolderSize(String backendName, String bucketName, String folderPath);
}
