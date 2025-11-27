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
            // 使用默认后端的名称（配置中的key，不是显示名称）
            String backendKey = storageService.getDefaultBackendKey();
            List<String> buckets = storageService.listBuckets(backendKey);
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
            String backendKey = storageService.getDefaultBackendKey();

            FileListDTO listDTO = new FileListDTO();
            listDTO.setBackendName(backendKey);
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
            String backendKey = storageService.getDefaultBackendKey();
            Map<String, Object> result = storageService.uploadFile(file, backendKey, bucketName, objectKey);
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
            String backendKey = storageService.getDefaultBackendKey();
            storageService.downloadFile(backendKey, bucketName, objectKey, response);
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
            String backendKey = storageService.getDefaultBackendKey();
            storageService.deleteFile(backendKey, bucketName, objectKey);
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
            String backendKey = storageService.getDefaultBackendKey();
            String bucketName = (String) request.get("bucketName");
            @SuppressWarnings("unchecked")
            List<String> objectKeys = (List<String>) request.get("objectKeys");

            storageService.batchDeleteFiles(backendKey, bucketName, objectKeys);
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
            String backendKey = storageService.getDefaultBackendKey();
            storageService.createFolder(backendKey, bucketName, folderPath);
            return Result.success();
        } catch (Exception e) {
            log.error("创建文件夹失败", e);
            return Result.error("创建文件夹失败: " + e.getMessage());
        }
    }

    /**
     * 调试接口 - 列出所有文件(不分页,不过滤)
     */
    @GetMapping("/debug/all-files")
    public Result<Map<String, Object>> debugListAllFiles(
            @RequestParam(required = false, defaultValue = "10") int maxKeys) {
        try {
            String backendKey = storageService.getDefaultBackendKey();

            FileListDTO listDTO = new FileListDTO();
            listDTO.setBackendName(backendKey);
            listDTO.setBucketName(null); // 使用默认bucket
            listDTO.setPrefix(""); // 不设置前缀,列出所有
            listDTO.setDelimiter(""); // 不设置分隔符,不分层
            listDTO.setPageSize(maxKeys);

            Map<String, Object> result = storageService.listFiles(listDTO);
            return Result.success(result);
        } catch (Exception e) {
            log.error("调试列出所有文件失败", e);
            return Result.error("调试列出所有文件失败: " + e.getMessage());
        }
    }

    /**
     * 调试接口 - 查找特定文件
     */
    @GetMapping("/debug/find-file")
    public Result<Map<String, Object>> debugFindFile(
            @RequestParam String objectKey) {
        try {
            String backendKey = storageService.getDefaultBackendKey();

            // 使用搜索功能查找文件
            FileListDTO listDTO = new FileListDTO();
            listDTO.setBackendName(backendKey);
            listDTO.setBucketName(null); // 使用默认bucket

            // 从objectKey提取目录和文件名
            String prefix = "";
            String fileName = objectKey;
            int lastSlash = objectKey.lastIndexOf('/');
            if (lastSlash > 0) {
                prefix = objectKey.substring(0, lastSlash + 1);
                fileName = objectKey.substring(lastSlash + 1);
            }

            listDTO.setPrefix(prefix);
            listDTO.setDelimiter("");
            listDTO.setPageSize(1000);

            Map<String, Object> result = storageService.listFiles(listDTO);

            // 查找匹配的文件
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> files = (List<Map<String, Object>>) result.get("files");

            Map<String, Object> response = new java.util.HashMap<>();
            response.put("searchKey", objectKey);
            response.put("searchPrefix", prefix);
            response.put("searchFileName", fileName);
            response.put("totalFiles", files != null ? files.size() : 0);

            if (files != null) {
                Map<String, Object> found = files.stream()
                    .filter(f -> objectKey.equals(f.get("key")))
                    .findFirst()
                    .orElse(null);

                response.put("found", found != null);
                response.put("fileInfo", found);

                // 返回前10个文件用于参考
                response.put("nearbyFiles", files.stream()
                    .limit(10)
                    .map(f -> f.get("key"))
                    .collect(java.util.stream.Collectors.toList()));
            }

            return Result.success(response);
        } catch (Exception e) {
            log.error("查找文件失败 - objectKey: {}", objectKey, e);
            return Result.error("查找文件失败: " + e.getMessage());
        }
    }

    /**
     * 重命名文件或文件夹
     */
    @PutMapping("/files/rename")
    public Result<Void> renameFile(
            @RequestParam String bucketName,
            @RequestParam String oldKey,
            @RequestParam String newKey) {
        try {
            String backendKey = storageService.getDefaultBackendKey();
            storageService.renameFile(backendKey, bucketName, oldKey, newKey);
            return Result.success();
        } catch (Exception e) {
            log.error("重命名文件失败 - oldKey: {}, newKey: {}", oldKey, newKey, e);
            return Result.error("重命名文件失败: " + e.getMessage());
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
            String backendKey = storageService.getDefaultBackendKey();

            FileListDTO listDTO = new FileListDTO();
            listDTO.setBackendName(backendKey);
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

    /**
     * 获取文件的预签名URL（用于分享）
     */
    @GetMapping("/share")
    public Result<Map<String, Object>> getShareUrl(
            @RequestParam String bucketName,
            @RequestParam String objectKey,
            @RequestParam(defaultValue = "3600") int expirationSeconds) {
        try {
            String backendKey = storageService.getDefaultBackendKey();

            // 验证过期时间（最长7天）
            if (expirationSeconds > 7 * 24 * 3600) {
                return Result.error("过期时间不能超过7天");
            }
            if (expirationSeconds < 60) {
                return Result.error("过期时间不能少于60秒");
            }

            String presignedUrl = storageService.getPresignedUrl(backendKey, bucketName, objectKey, expirationSeconds);

            Map<String, Object> result = new java.util.HashMap<>();
            result.put("url", presignedUrl);
            result.put("expirationSeconds", expirationSeconds);
            result.put("expiresAt", System.currentTimeMillis() + (expirationSeconds * 1000L));

            return Result.success(result);
        } catch (Exception e) {
            log.error("获取分享链接失败 - bucketName: {}, objectKey: {}", bucketName, objectKey, e);
            return Result.error("获取分享链接失败: " + e.getMessage());
        }
    }

    /**
     * 计算文件夹大小
     */
    @GetMapping("/folder/size")
    public Result<Map<String, Object>> calculateFolderSize(
            @RequestParam String bucketName,
            @RequestParam String folderPath) {
        try {
            String backendKey = storageService.getDefaultBackendKey();

            // 确保文件夹路径以/结尾
            if (!folderPath.endsWith("/")) {
                folderPath += "/";
            }

            long totalSize = storageService.calculateFolderSize(backendKey, bucketName, folderPath);

            Map<String, Object> result = new java.util.HashMap<>();
            result.put("folderPath", folderPath);
            result.put("totalSize", totalSize);

            return Result.success(result);
        } catch (Exception e) {
            log.error("计算文件夹大小失败 - bucketName: {}, folderPath: {}", bucketName, folderPath, e);
            return Result.error("计算文件夹大小失败: " + e.getMessage());
        }
    }
}
