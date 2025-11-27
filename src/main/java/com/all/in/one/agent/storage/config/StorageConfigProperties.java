package com.all.in.one.agent.storage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 存储配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "storage")
public class StorageConfigProperties {

    /**
     * 存储后端配置（支持多个）
     * key为配置名称，value为具体配置
     */
    private Map<String, Backend> backends = new HashMap<>();

    /**
     * 默认使用的存储后端名称
     */
    private String defaultBackend;

    /**
     * 文件上传配置
     */
    private Upload upload = new Upload();

    /**
     * 文件预览配置
     */
    private Preview preview = new Preview();

    /**
     * 安全配置
     */
    private Security security = new Security();

    /**
     * 缓存配置
     */
    private Cache cache = new Cache();

    @Data
    public static class Backend {
        /**
         * 配置名称
         */
        private String name;

        /**
         * 存储类型：S3, MINIO, OSS等
         */
        private String type;

        /**
         * 端点URL
         */
        private String endpoint;

        /**
         * 访问密钥ID
         */
        private String accessKeyId;

        /**
         * 访问密钥
         */
        private String accessKeySecret;

        /**
         * 区域
         */
        private String region;

        /**
         * 默认桶名
         */
        private String defaultBucket;

        /**
         * 是否启用
         */
        private Boolean enabled = true;
    }
    
    @Data
    public static class Upload {
        /**
         * 最大文件大小（字节）
         */
        private long maxFileSize = 100 * 1024 * 1024; // 100MB

        /**
         * 临时文件目录
         */
        private String tempDir = "/tmp/storage";
    }
    
    @Data
    public static class Preview {
        /**
         * 是否启用文件预览
         */
        private boolean enabled = true;

        /**
         * 预览URL过期时间（秒）
         */
        private int urlExpiration = 3600; // 1小时
    }
    
    @Data
    public static class Security {
        /**
         * 是否启用文件类型检查
         */
        private boolean enableFileTypeCheck = true;

        /**
         * 是否启用文件大小限制
         */
        private boolean enableFileSizeLimit = true;

        /**
         * 是否启用文件名清理
         */
        private boolean enableFilenameSanitization = true;

        /**
         * 允许的文件类型
         */
        private String[] allowedFileTypes = {
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "txt", "md", "json", "xml", "yaml", "yml",
            "zip", "rar", "7z", "tar", "gz",
            "mp3", "wav", "ogg", "flac",
            "mp4", "avi", "mov", "wmv", "flv", "webm"
        };

        /**
         * 禁止的文件类型
         */
        private String[] forbiddenFileTypes = {
            "exe", "bat", "cmd", "com", "pif", "scr", "jar", "js", "vbs", "sh"
        };
    }
    
    @Data
    public static class Cache {
        /**
         * 是否启用缓存
         */
        private boolean enabled = true;
        
        /**
         * 缓存过期时间（秒）
         */
        private int expiration = 300; // 5分钟
        
        /**
         * 最大缓存条目数
         */
        private int maxEntries = 1000;
        
        /**
         * 缓存目录
         */
        private String cacheDir = "/tmp/storage-cache";
    }
} 