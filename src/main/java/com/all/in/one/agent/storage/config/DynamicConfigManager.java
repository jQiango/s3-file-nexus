package com.all.in.one.agent.storage.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态配置管理器
 * 用于运行时更新 S3 配置
 */
@Slf4j
@Component
public class DynamicConfigManager {

    private final StorageConfigProperties configProperties;
    private final Map<String, StorageConfigProperties.Backend> runtimeBackends = new ConcurrentHashMap<>();

    public DynamicConfigManager(StorageConfigProperties configProperties) {
        this.configProperties = configProperties;
    }

    /**
     * 更新动态配置
     */
    public void updateDynamicConfig(String endpoint, String accessKey, String secretKey,
                                   String region, String defaultBucket) {
        StorageConfigProperties.Backend backend = new StorageConfigProperties.Backend();
        backend.setName("Dynamic Config");
        backend.setType("S3");
        backend.setEndpoint(endpoint);
        backend.setAccessKeyId(accessKey);
        backend.setAccessKeySecret(secretKey);
        backend.setRegion(region != null && !region.isEmpty() ? region : "us-east-1");
        backend.setDefaultBucket(defaultBucket);
        backend.setEnabled(true);

        // 更新到运行时配置
        runtimeBackends.put("dynamic", backend);

        // 同时更新 configProperties 中的配置
        configProperties.getBackends().put("dynamic", backend);

        log.info("动态配置已更新: endpoint={}, region={}, bucket={}", endpoint, region, defaultBucket);
    }

    /**
     * 获取动态配置
     */
    public StorageConfigProperties.Backend getDynamicConfig() {
        return runtimeBackends.get("dynamic");
    }

    /**
     * 清除动态配置
     */
    public void clearDynamicConfig() {
        runtimeBackends.remove("dynamic");
        log.info("动态配置已清除");
    }
}
