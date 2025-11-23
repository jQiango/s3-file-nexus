package com.all.in.one.agent.storage.controller;

import com.all.in.one.agent.storage.common.Result;
import com.all.in.one.agent.storage.config.StorageConfigProperties;
import com.all.in.one.agent.storage.dto.FileListDTO;
import com.all.in.one.agent.storage.service.StorageService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 存储控制器 - 核心API
 */
@Slf4j
@RestController
@RequestMapping("/api/storage")
@CrossOrigin(origins = "*")
public class StorageController {

    private final StorageService storageService;

    public StorageController(StorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * 获取默认存储后端配置
     */
    @GetMapping("/backend")
    public Result<StorageConfigProperties.Backend> getDefaultBackend() {
        try {
            StorageConfigProperties.Backend backend = storageService.getDefaultBackend();
            return Result.success(backend);
        } catch (Exception e) {
            log.error("获取默认存储后端失败", e);
            return Result.error("获取默认存储后端失败: " + e.getMessage());
        }
    }

    /**
     * 获取存储桶列表
     */
    @GetMapping("/buckets")
    public Result<List<String>> listBuckets() {
        try {
            String backendName = storageService.getDefaultBackend().getName();
            List<String> buckets = storageService.listBuckets(backendName);
            return Result.success(buckets);
        } catch (Exception e) {
            log.error("获取存储桶列表失败", e);
            return Result.error("获取存储桶列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件列表
     */
    @PostMapping("/files/list")
    public Result<Map<String, Object>> listFiles(@RequestBody Map<String, Object> request) {
        try {
            String backendName = storageService.getDefaultBackend().getName();

            FileListDTO listDTO = new FileListDTO();
            listDTO.setBackendName(backendName);
            listDTO.setBucketName((String) request.get("bucketName"));
            listDTO.setPrefix((String) request.get("prefix"));
            listDTO.setDelimiter((String) request.get("delimiter"));
            listDTO.setContinuationToken((String) request.get("continuationToken"));

            if (request.get("pageSize") != null) {
                listDTO.setPageSize(((Number) request.get("pageSize")).intValue());
            }

            Map<String, Object> result = storageService.listFiles(listDTO);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取文件列表失败", e);
            return Result.error("获取文件列表失败: " + e.getMessage());
        }
    }

    /**
     * 上传文件
     */
    @PostMapping("/upload")
    public Result<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "bucketName", required = false) String bucketName,
            @RequestParam(value = "objectKey", required = false) String objectKey) {
        try {
            String backendName = storageService.getDefaultBackend().getName();
            Map<String, Object> result = storageService.uploadFile(file, backendName, bucketName, objectKey);
            return Result.success(result);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 下载文件
     */
    @GetMapping("/download")
    public void downloadFile(
            @RequestParam String bucketName,
            @RequestParam String objectKey,
            HttpServletResponse response) {
        try {
            String backendName = storageService.getDefaultBackend().getName();
            storageService.downloadFile(backendName, bucketName, objectKey, response);
        } catch (Exception e) {
            log.error("文件下载失败", e);
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("文件下载失败: " + e.getMessage());
            } catch (Exception ex) {
                log.error("写入错误响应失败", ex);
            }
        }
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/files")
    public Result<Void> deleteFile(
            @RequestParam String bucketName,
            @RequestParam String objectKey) {
        try {
            String backendName = storageService.getDefaultBackend().getName();
            storageService.deleteFile(backendName, bucketName, objectKey);
            return Result.success();
        } catch (Exception e) {
            log.error("删除文件失败", e);
            return Result.error("删除文件失败: " + e.getMessage());
        }
    }

    /**
     * 批量删除文件
     */
    @DeleteMapping("/files/batch")
    public Result<Void> batchDeleteFiles(@RequestBody Map<String, Object> request) {
        try {
            String backendName = storageService.getDefaultBackend().getName();
            String bucketName = (String) request.get("bucketName");
            @SuppressWarnings("unchecked")
            List<String> objectKeys = (List<String>) request.get("objectKeys");

            storageService.batchDeleteFiles(backendName, bucketName, objectKeys);
            return Result.success();
        } catch (Exception e) {
            log.error("批量删除文件失败", e);
            return Result.error("批量删除文件失败: " + e.getMessage());
        }
    }

    /**
     * 创建文件夹
     */
    @PostMapping("/folder")
    public Result<Void> createFolder(
            @RequestParam String bucketName,
            @RequestParam String folderPath) {
        try {
            String backendName = storageService.getDefaultBackend().getName();
            storageService.createFolder(backendName, bucketName, folderPath);
            return Result.success();
        } catch (Exception e) {
            log.error("创建文件夹失败", e);
            return Result.error("创建文件夹失败: " + e.getMessage());
        }
    }

    /**
     * 搜索文件
     */
    @GetMapping("/search")
    public Result<Map<String, Object>> searchFiles(
            @RequestParam String bucketName,
            @RequestParam String keyword,
            @RequestParam(required = false) String prefix,
            @RequestParam(defaultValue = "50") int maxResults) {
        try {
            String backendName = storageService.getDefaultBackend().getName();

            FileListDTO listDTO = new FileListDTO();
            listDTO.setBackendName(backendName);
            listDTO.setBucketName(bucketName);
            listDTO.setPrefix(prefix);
            listDTO.setPageSize(500);

            String continuationToken = null;
            List<Map<String, Object>> matched = new java.util.ArrayList<>();
            int safetyPages = 0;

            do {
                listDTO.setContinuationToken(continuationToken);
                Map<String, Object> page = storageService.listFiles(listDTO);

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> files = (List<Map<String, Object>>) page.get("files");
                if (files != null) {
                    for (Map<String, Object> file : files) {
                        String key = (String) file.get("key");
                        if (key != null && key.toLowerCase().contains(keyword.toLowerCase())) {
                            matched.add(file);
                            if (matched.size() >= maxResults) break;
                        }
                    }
                }

                if (matched.size() >= maxResults) break;

                @SuppressWarnings("unchecked")
                Map<String, Object> pagination = (Map<String, Object>) page.get("pagination");
                boolean hasMore = pagination != null && Boolean.TRUE.equals(pagination.get("hasMore"));
                continuationToken = pagination != null ? (String) pagination.get("nextContinuationToken") : null;

                if (++safetyPages > 2000) break;
                if (!hasMore) break;
            } while (continuationToken != null);

            Map<String, Object> searchResult = new java.util.HashMap<>();
            searchResult.put("files", matched.size() > maxResults ? matched.subList(0, maxResults) : matched);
            searchResult.put("totalFound", matched.size());
            searchResult.put("keyword", keyword);

            return Result.success(searchResult);
        } catch (Exception e) {
            log.error("搜索文件失败", e);
            return Result.error("搜索文件失败: " + e.getMessage());
        }
    }
}
