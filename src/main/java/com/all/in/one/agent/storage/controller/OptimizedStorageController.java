package com.all.in.one.agent.storage.controller;

import com.all.in.one.agent.storage.common.Result;
import com.all.in.one.agent.storage.dto.FileItem;
import com.all.in.one.agent.storage.dto.FileListResponse;
import com.all.in.one.agent.storage.dto.FolderStats;
import com.all.in.one.agent.storage.service.StorageService;
import com.all.in.one.agent.storage.util.S3ClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.s3.model.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 优化后的存储控制器
 *
 * 功能：
 * 1. 混合列表接口（文件夹+文件一次返回）
 * 2. 本地内存缓存（Caffeine）
 * 3. ETag HTTP缓存
 * 4. 文件夹统计信息
 * 5. 智能排序（文件夹优先）
 */
@Slf4j
@RestController
@RequestMapping("/api/storage/v2")
@CrossOrigin(origins = "*")
public class OptimizedStorageController {

    private final StorageService storageService;
    private final S3ClientUtil s3ClientUtil;
    private final org.springframework.cache.CacheManager cacheManager;

    public OptimizedStorageController(
            StorageService storageService,
            S3ClientUtil s3ClientUtil,
            org.springframework.cache.CacheManager cacheManager) {
        this.storageService = storageService;
        this.s3ClientUtil = s3ClientUtil;
        this.cacheManager = cacheManager;
        log.info("优化存储控制器已启动 - 使用本地内存缓存（Caffeine）");
    }

    /**
     * 清除缓存（调试用）
     */
    @DeleteMapping("/cache")
    public ResponseEntity<Result<String>> clearCache() {
        int cleared = 0;
        for (String cacheName : cacheManager.getCacheNames()) {
            org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                cleared++;
                log.info("已清除缓存: {}", cacheName);
            }
        }
        return ResponseEntity.ok(Result.success("已清除 " + cleared + " 个缓存"));
    }

    /**
     * 优化后的文件列表接口
     *
     * @param bucket 存储桶名称
     * @param prefix 前缀（目录路径）
     * @param continuationToken 分页token
     * @param pageSize 每页数量
     * @return 文件列表响应
     */
    @GetMapping("/list")
    public ResponseEntity<Result<FileListResponse>> listFiles(
            @RequestParam String bucket,
            @RequestParam(required = false, defaultValue = "") String prefix,
            @RequestParam(required = false) String continuationToken,
            @RequestParam(defaultValue = "100") int pageSize,
            @RequestParam(required = false) Long _t) {  // 时间戳参数，用于绕过缓存

        try {
            // 从缓存或S3加载（使用@Cacheable自动缓存）
            // 注意：不使用ETag，因为它与分页冲突
            // 依赖Caffeine本地缓存已经足够快（2ms）
            FileListResponse response = loadFileList(bucket, prefix, continuationToken, pageSize);

            // 返回结果（禁用浏览器缓存，只使用服务器端Caffeine缓存）
            return ResponseEntity
                    .ok()
                    .cacheControl(org.springframework.http.CacheControl.noCache().noStore())
                    .body(Result.success(response));

        } catch (Exception e) {
            log.error("获取文件列表失败 - bucket: {}, prefix: {}", bucket, prefix, e);
            return ResponseEntity
                    .ok()
                    .body(Result.error("获取文件列表失败: " + e.getMessage()));
        }
    }

    /**
     * 加载文件列表（带缓存）
     *
     * Spring Cache会自动处理：
     * - 首次调用：执行方法，缓存结果
     * - 后续调用：直接返回缓存（5分钟内）
     */
    @Cacheable(
        value = "fileList",
        key = "#bucket + ':' + #prefix + ':' + (#continuationToken != null ? #continuationToken : '') + ':' + #pageSize"
    )
    public FileListResponse loadFileList(String bucket, String prefix, String continuationToken, int pageSize) {
        log.debug("从S3加载文件列表 - bucket: {}, prefix: {}, token: {}", bucket, prefix, continuationToken);

        FileListResponse response = loadFromS3(bucket, prefix, continuationToken, pageSize);

        // 智能排序：文件夹优先，然后按名称
        response.setItems(sortItems(response.getItems()));
        response.setFromCache(false);  // 首次加载标记为非缓存

        return response;
    }

    /**
     * 从S3加载混合内容（文件夹+文件）
     */
    private FileListResponse loadFromS3(String bucket, String prefix, String continuationToken, int pageSize) {
        try {
            String backendKey = storageService.getDefaultBackendKey();
            var backend = storageService.getBackend(backendKey);
            var s3Client = s3ClientUtil.createS3Client(backend);

            // 使用V1 API（ListObjects）因为某些S3服务的V2 API不返回CommonPrefixes
            ListObjectsRequest.Builder requestBuilder = ListObjectsRequest.builder()
                    .bucket(bucket)
                    .prefix(prefix)
                    .delimiter("/")  // 重要：让S3返回文件夹
                    .maxKeys(pageSize);

            if (continuationToken != null && !continuationToken.isEmpty()) {
                requestBuilder.marker(continuationToken);  // V1使用marker而不是continuationToken
            }

            ListObjectsResponse response = s3Client.listObjects(requestBuilder.build());

            List<FileItem> items = new ArrayList<>();

            log.info("S3响应 - bucket: {}, prefix: '{}', CommonPrefixes: {}, Contents: {}",
                    bucket, prefix, response.commonPrefixes().size(), response.contents().size());

            // 添加文件夹（CommonPrefixes）
            for (CommonPrefix commonPrefix : response.commonPrefixes()) {
                // 异步计算文件夹统计信息（不阻塞主流程）
                FolderStats stats = FolderStats.builder()
                        .calculating(true)
                        .build();

                // 启动异步任务计算统计信息
                String folderPrefix = commonPrefix.prefix();
                CompletableFuture.runAsync(() -> {
                    try {
                        calculateFolderStats(backendKey, bucket, folderPrefix);
                    } catch (Exception e) {
                        log.warn("计算文件夹统计信息失败: {}", folderPrefix, e);
                    }
                });

                items.add(FileItem.folder(folderPrefix, stats));
            }

            // 添加文件
            for (S3Object s3Object : response.contents()) {
                // 跳过"文件夹标记文件"（以/结尾的0字节文件）
                if (s3Object.key().endsWith("/")) {
                    continue;
                }

                FileItem fileItem = FileItem.builder()
                        .name(extractFileName(s3Object.key()))
                        .key(s3Object.key())
                        .type("file")
                        .size(s3Object.size())
                        .lastModified(s3Object.lastModified())
                        .build();

                items.add(fileItem);
            }

            return FileListResponse.builder()
                    .items(items)
                    .nextContinuationToken(response.nextMarker())  // V1使用nextMarker
                    .isTruncated(response.isTruncated())
                    .totalCount(items.size())
                    .fromCache(false)
                    .build();

        } catch (Exception e) {
            log.error("从S3加载失败", e);
            throw new RuntimeException("从S3加载失败: " + e.getMessage(), e);
        }
    }

    /**
     * 计算文件夹统计信息（带缓存，10分钟）
     */
    @Cacheable(
        value = "folderStats",
        key = "#bucket + ':' + #prefix"
    )
    public FolderStats calculateFolderStats(String backendKey, String bucket, String prefix) {
        try {
            log.debug("计算文件夹统计 - bucket: {}, prefix: {}", bucket, prefix);

            // 获取S3客户端
            var backend = storageService.getBackend(backendKey);
            var s3Client = s3ClientUtil.createS3Client(backend);

            // 计算统计信息（限制扫描1000个文件避免性能问题）
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(prefix)
                    .maxKeys(1000)
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(request);

            int fileCount = (int) response.contents().stream()
                    .filter(obj -> !obj.key().endsWith("/"))
                    .count();

            long totalSize = response.contents().stream()
                    .filter(obj -> !obj.key().endsWith("/"))
                    .mapToLong(S3Object::size)
                    .sum();

            return FolderStats.builder()
                    .fileCount(fileCount)
                    .totalSize(totalSize)
                    .calculating(false)
                    .build();

        } catch (Exception e) {
            log.error("计算文件夹统计信息失败", e);
            return FolderStats.builder()
                    .calculating(false)
                    .build();
        }
    }

    /**
     * 智能排序：文件夹优先，然后按时间倒序（最新的在前）
     */
    private List<FileItem> sortItems(List<FileItem> items) {
        return items.stream()
                .sorted(Comparator
                        // 1. 文件夹优先
                        .comparing(FileItem::isFolder, Comparator.reverseOrder())
                        // 2. 按最后修改时间倒序（最新的在前，null值放最后）
                        .thenComparing(FileItem::getLastModified,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }


    /**
     * 提取文件名（去掉路径）
     */
    private String extractFileName(String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        int lastSlash = key.lastIndexOf('/');
        return lastSlash >= 0 ? key.substring(lastSlash + 1) : key;
    }
}
