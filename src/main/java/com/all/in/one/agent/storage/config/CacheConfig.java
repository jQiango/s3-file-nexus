package com.all.in.one.agent.storage.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 本地缓存配置
 *
 * 使用Caffeine作为本地内存缓存
 * 优点：
 * 1. 无需外部依赖（无需Redis）
 * 2. 配置简单
 * 3. 性能优秀（比Redis略快）
 *
 * 缺点：
 * 1. 重启应用缓存丢失
 * 2. 多实例不共享缓存
 * 3. 内存占用在应用内
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 配置Caffeine缓存管理器
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // 配置缓存行为
        cacheManager.setCaffeine(Caffeine.newBuilder()
                // 初始容量
                .initialCapacity(100)
                // 最大容量（超过会自动驱逐最少使用的）
                .maximumSize(1000)
                // 写入后5分钟过期
                .expireAfterWrite(5, TimeUnit.MINUTES)
                // 启用统计（可选，用于监控）
                .recordStats()
        );

        return cacheManager;
    }

    /**
     * 专门用于文件夹统计的缓存（更长过期时间）
     */
    @Bean
    public Caffeine<Object, Object> folderStatsCaffeine() {
        return Caffeine.newBuilder()
                .initialCapacity(50)
                .maximumSize(500)
                .expireAfterWrite(10, TimeUnit.MINUTES)  // 10分钟过期
                .recordStats();
    }
}
