package com.all.in.one.agent.storage.service.impl;

import com.all.in.one.agent.storage.config.StorageConfigProperties;
import com.all.in.one.agent.storage.dto.FileListDTO;
import com.all.in.one.agent.storage.service.StorageService;
import com.all.in.one.agent.storage.util.S3ClientUtil;
import com.all.in.one.agent.storage.security.FileSecurityUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 存储服务实现类
 */
@Slf4j
@Service
public class StorageServiceImpl implements StorageService {

    private final StorageConfigProperties configProperties;
    private final S3ClientUtil s3ClientUtil;
    private final FileSecurityUtils fileSecurityUtils;

    public StorageServiceImpl(StorageConfigProperties configProperties, S3ClientUtil s3ClientUtil, FileSecurityUtils fileSecurityUtils) {
        this.configProperties = configProperties;
        this.s3ClientUtil = s3ClientUtil;
        this.fileSecurityUtils = fileSecurityUtils;
    }

    @Override
    public Map<String, StorageConfigProperties.Backend> getAllBackends() {
        return configProperties.getBackends();
    }

    @Override
    public StorageConfigProperties.Backend getBackend(String backendName) {
        StorageConfigProperties.Backend backend = configProperties.getBackends().get(backendName);
        if (backend == null) {
            throw new RuntimeException("存储后端不存在，请先配置 S3 连接信息: " + backendName);
        }
        if (!Boolean.TRUE.equals(backend.getEnabled())) {
            throw new RuntimeException("存储后端未启用: " + backendName);
        }
        // 检查必要的配置是否完整
        if (backend.getAccessKeyId() == null || backend.getAccessKeyId().isEmpty()) {
            throw new RuntimeException("Access Key ID 未配置，请先配置 S3 连接信息");
        }
        if (backend.getAccessKeySecret() == null || backend.getAccessKeySecret().isEmpty()) {
            throw new RuntimeException("Secret Access Key 未配置，请先配置 S3 连接信息");
        }
        if (backend.getEndpoint() == null || backend.getEndpoint().isEmpty()) {
            throw new RuntimeException("Endpoint 未配置，请先配置 S3 连接信息");
        }
        return backend;
    }

    @Override
    public StorageConfigProperties.Backend getDefaultBackend() {
        String defaultBackendName = configProperties.getDefaultBackend();
        if (defaultBackendName == null || defaultBackendName.isEmpty()) {
            throw new RuntimeException("未配置默认存储后端");
        }
        return getBackend(defaultBackendName);
    }

    @Override
    public String getDefaultBackendKey() {
        String defaultBackendKey = configProperties.getDefaultBackend();
        if (defaultBackendKey == null || defaultBackendKey.isEmpty()) {
            throw new RuntimeException("未配置默认存储后端");
        }
        return defaultBackendKey;
    }

    @Override
    public boolean testConnection(String backendName) {
        try {
            StorageConfigProperties.Backend backend = getBackend(backendName);
            S3Client s3Client = s3ClientUtil.createS3Client(backend);
            ListBucketsRequest request = ListBucketsRequest.builder().build();
            s3Client.listBuckets(request);
            return true;
        } catch (Exception e) {
            log.error("测试存储连接失败 - backend: {}", backendName, e);
            return false;
        }
    }

    @Override
    public Map<String, Object> uploadFile(MultipartFile file, String backendName, String bucketName, String objectKey) {
        StorageConfigProperties.Backend backend = getBackend(backendName);

        // 文件安全检查
        if (!fileSecurityUtils.isFileSecure(file.getOriginalFilename(), file.getSize())) {
            throw new RuntimeException("文件类型不安全或文件过大");
        }

        try {
            S3Client s3Client = s3ClientUtil.createS3Client(backend);

            String actualBucketName = bucketName != null ? bucketName : backend.getDefaultBucket();

            // 安全清理文件名
            String originalFilename = fileSecurityUtils.sanitizeFilename(file.getOriginalFilename());
            String actualObjectKey = objectKey != null ? objectKey : generateObjectKey(originalFilename);

            // 检查存储桶是否存在，如果不存在则创建
            try {
                HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                        .bucket(actualBucketName)
                        .build();
                s3Client.headBucket(headBucketRequest);
            } catch (NoSuchBucketException e) {
                // 存储桶不存在，创建它
                CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                        .bucket(actualBucketName)
                        .build();
                s3Client.createBucket(createBucketRequest);
                log.info("创建存储桶: {}", actualBucketName);
            }

            // 上传文件到S3
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(actualBucketName)
                    .key(actualObjectKey)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(
                    file.getInputStream(), file.getSize()));

            // 返回文件信息
            Map<String, Object> result = new HashMap<>();
            result.put("backendName", backendName);
            result.put("bucketName", actualBucketName);
            result.put("objectKey", actualObjectKey);
            result.put("fileName", file.getOriginalFilename());
            result.put("fileSize", file.getSize());
            result.put("contentType", file.getContentType());
            result.put("uploadTime", System.currentTimeMillis());

            return result;

        } catch (Exception e) {
            log.error("文件上传失败 - backend: {}, bucket: {}", backendName, bucketName, e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public void downloadFile(String backendName, String bucketName, String objectKey, HttpServletResponse response) {
        StorageConfigProperties.Backend backend = getBackend(backendName);

        try {
            S3Client s3Client = s3ClientUtil.createS3Client(backend);
            String actualBucketName = bucketName != null ? bucketName : backend.getDefaultBucket();

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(actualBucketName)
                    .key(objectKey)
                    .build();

            // 从对象键中提取文件名
            String fileName = objectKey.substring(objectKey.lastIndexOf('/') + 1);

            // 设置响应头
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=" +
                    URLEncoder.encode(fileName, StandardCharsets.UTF_8));

            // 复制文件流到响应
            s3Client.getObject(getObjectRequest, ResponseTransformer.toOutputStream(response.getOutputStream()));

        } catch (Exception e) {
            log.error("文件下载失败 - backend: {}, bucket: {}, key: {}", backendName, bucketName, objectKey, e);
            throw new RuntimeException("文件下载失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> listFiles(FileListDTO listDTO) {
        if (listDTO == null) {
            throw new RuntimeException("请求参数不能为空");
        }

        if (listDTO.getBackendName() == null || listDTO.getBackendName().isEmpty()) {
            throw new RuntimeException("存储后端名称不能为空");
        }

        StorageConfigProperties.Backend backend = getBackend(listDTO.getBackendName());

        try {
            S3Client s3Client = s3ClientUtil.createS3Client(backend);
            String bucketName = listDTO.getBucketName() != null ? listDTO.getBucketName() : backend.getDefaultBucket();

            log.debug("开始获取文件列表 - backend: {}, bucket: {}, prefix: {}, delimiter: {}",
                    listDTO.getBackendName(), bucketName, listDTO.getPrefix(), listDTO.getDelimiter());

            // 使用V1 API（ListObjects）因为链家S3的V2 API不返回CommonPrefixes
            var requestBuilder = ListObjectsRequest.builder()
                    .bucket(bucketName)
                    .maxKeys(listDTO.getPageSize() != null ? listDTO.getPageSize() : listDTO.getMaxKeys());

            if (listDTO.getPrefix() != null && !listDTO.getPrefix().isEmpty()) {
                requestBuilder = requestBuilder.prefix(listDTO.getPrefix());
                log.debug("设置前缀: {}", listDTO.getPrefix());
            }

            if (listDTO.getDelimiter() != null && !listDTO.getDelimiter().isEmpty()) {
                requestBuilder = requestBuilder.delimiter(listDTO.getDelimiter());
                log.debug("设置分隔符: {}", listDTO.getDelimiter());
            }

            if (listDTO.getContinuationToken() != null && !listDTO.getContinuationToken().isEmpty()) {
                requestBuilder = requestBuilder.marker(listDTO.getContinuationToken());  // V1使用marker代替continuationToken
                log.debug("设置继续标记: {}", listDTO.getContinuationToken());
            }

            ListObjectsRequest request = requestBuilder.build();
            log.debug("S3请求参数: {}", request);

            ListObjectsResponse response = s3Client.listObjects(request);

            log.debug("S3响应 - 文件数: {}, 文件夹数: {}, 截断: {}",
                    response.contents().size(),
                    response.commonPrefixes() != null ? response.commonPrefixes().size() : 0,
                    response.isTruncated());

            Map<String, Object> result = new HashMap<>();

            // 1. 提取所有一级文件夹和当前目录下的文件
            List<Map<String, Object>> allFolders = new ArrayList<>();
            List<Map<String, Object>> allFiles = new ArrayList<>();

            // 先用原有逻辑提取一级文件夹
            if (response.commonPrefixes() != null) {
                for (CommonPrefix commonPrefix : response.commonPrefixes()) {
                    Map<String, Object> folderInfo = new HashMap<>();
                    String prefix = commonPrefix.prefix();
                    String folderName = prefix;
                    if (listDTO.getPrefix() != null && !listDTO.getPrefix().isEmpty()) {
                        folderName = prefix.replace(listDTO.getPrefix(), "");
                    }
                    if (folderName.endsWith("/")) {
                        folderName = folderName.substring(0, folderName.length() - 1);
                    }
                    if (!folderName.isEmpty()) {
                        folderInfo.put("key", prefix);
                        folderInfo.put("name", folderName);
                        folderInfo.put("isFolder", true);
                        folderInfo.put("lastModified", null);
                        folderInfo.put("size", 0L);
                        allFolders.add(folderInfo);
                    }
                }
            }
            // 补充：包含空目录占位对象（key 以 '/' 结尾且直接位于当前前缀下）
            // 以及 Size=0 的根级对象（rclone风格的文件夹标记，不带斜杠）
            for (S3Object obj : response.contents()) {
                String key = obj.key();

                // 处理带斜杠的文件夹标记（标准S3风格）
                if (key.endsWith("/")) {
                    String relative = (listDTO.getPrefix() != null && !listDTO.getPrefix().isEmpty())
                            ? (key.startsWith(listDTO.getPrefix()) ? key.substring(listDTO.getPrefix().length()) : null)
                            : key;
                    if (relative != null && !relative.isEmpty() && !relative.contains("/")) {
                        boolean exists = allFolders.stream().anyMatch(f -> key.equals(f.get("key")));
                        if (!exists) {
                            Map<String, Object> folderInfo = new HashMap<>();
                            folderInfo.put("key", key);
                            folderInfo.put("name", relative.substring(0, relative.length() - 1));
                            folderInfo.put("isFolder", true);
                            folderInfo.put("lastModified", null);
                            folderInfo.put("size", 0L);
                            allFolders.add(folderInfo);
                        }
                    }
                }
                // 处理不带斜杠的文件夹标记（Size=0 且在根目录，rclone风格）
                // 注意：只在没有prefix的情况下处理根级别的0字节对象作为文件夹
                else if ((listDTO.getPrefix() == null || listDTO.getPrefix().isEmpty())
                         && obj.size() != null && obj.size() == 0L
                         && !key.contains("/")) {
                    boolean exists = allFolders.stream().anyMatch(f ->
                        key.equals(f.get("key")) || (key + "/").equals(f.get("key"))
                    );
                    if (!exists) {
                        log.info("发现0字节文件夹标记: {}", key);
                        Map<String, Object> folderInfo = new HashMap<>();
                        folderInfo.put("key", key + "/"); // 添加斜杠以保持一致性
                        folderInfo.put("name", key);
                        folderInfo.put("isFolder", true);
                        folderInfo.put("lastModified", obj.lastModified());
                        folderInfo.put("size", 0L);
                        allFolders.add(folderInfo);
                    }
                }
            }
            // 如果没有commonPrefixes，尝试从文件列表中提取
            // 当数据被截断时，需要遍历所有页面以发现所有文件夹
            if (allFolders.isEmpty()) {
                Set<String> folderSet = new HashSet<>();
                String marker = null;
                int pageCount = 0;
                int maxPages = 100; // 安全限制，避免无限循环
                ListObjectsResponse currentResponse = response;

                log.debug("开始提取文件夹（分页模式），数据截断: {}", currentResponse.isTruncated());

                do {
                    pageCount++;
                    log.info("处理第 {} 页，文件数: {}", pageCount, currentResponse.contents().size());

                    // 从当前页提取文件夹
                    for (S3Object obj : currentResponse.contents()) {
                        String key = obj.key();

                        // 方法1：从包含斜杠的路径中提取文件夹前缀
                        if (key.contains("/")) {
                            String folderPath;
                            if (listDTO.getPrefix() == null || listDTO.getPrefix().isEmpty()) {
                                folderPath = key.substring(0, key.indexOf("/") + 1);
                            } else {
                                if (key.startsWith(listDTO.getPrefix())) {
                                    String relativePath = key.substring(listDTO.getPrefix().length());
                                    if (relativePath.contains("/")) {
                                        String subFolder = relativePath.substring(0, relativePath.indexOf("/") + 1);
                                        folderPath = listDTO.getPrefix() + subFolder;
                                    } else {
                                        continue;
                                    }
                                } else {
                                    continue;
                                }
                            }
                            folderSet.add(folderPath);
                        }
                        // 方法2：0字节对象作为文件夹标记（rclone风格）
                        else if (obj.size() != null && obj.size() == 0L && !key.endsWith("/")) {
                            // 检查是否在当前前缀下
                            if (listDTO.getPrefix() == null || listDTO.getPrefix().isEmpty() || key.startsWith(listDTO.getPrefix())) {
                                String folderPath = key + "/";
                                folderSet.add(folderPath);
                                log.info("发现0字节文件夹标记: {}", key);
                            }
                        }
                    }

                    // 如果还有更多数据，继续获取下一页 (V1 API使用marker)
                    if (currentResponse.isTruncated() && pageCount < maxPages) {
                        marker = currentResponse.nextMarker();
                        log.info("数据被截断，获取下一页，marker: {}", marker);

                        ListObjectsRequest nextRequest = requestBuilder
                                .marker(marker)
                                .build();
                        currentResponse = s3Client.listObjects(nextRequest);
                    } else {
                        break;
                    }

                } while (currentResponse.isTruncated() && pageCount < maxPages);

                log.info("文件夹提取完成，总共处理 {} 页，发现 {} 个唯一文件夹", pageCount, folderSet.size());

                // 转换folderSet为文件夹信息列表
                for (String folderPath : folderSet) {
                    String folderName = folderPath;
                    if (listDTO.getPrefix() != null && !listDTO.getPrefix().isEmpty()) {
                        folderName = folderPath.replace(listDTO.getPrefix(), "");
                    }
                    if (folderName.endsWith("/")) {
                        folderName = folderName.substring(0, folderName.length() - 1);
                    }
                    if (!folderName.isEmpty()) {
                        Map<String, Object> folderInfo = new HashMap<>();
                        folderInfo.put("key", folderPath);
                        folderInfo.put("name", folderName);
                        folderInfo.put("isFolder", true);
                        folderInfo.put("lastModified", null);
                        folderInfo.put("size", 0L);
                        allFolders.add(folderInfo);
                    }
                }

                log.debug("文件夹提取完成，共 {} 个", allFolders.size());
            }

            // 提取当前目录下的文件
            // 注意：当设置了delimiter='/'时，S3 API已经自动过滤了子目录中的文件
            // contents只包含当前prefix下的直接文件，不包括子目录中的文件
            // 因此我们不需要再做额外的过滤，信任S3返回的结果即可
            for (S3Object obj : response.contents()) {
                String key = obj.key();

                // 跳过文件夹占位符（以/结尾的对象）
                if (key.endsWith("/")) {
                    continue;
                }

                // 跳过prefix本身（如果prefix是一个文件）
                if (listDTO.getPrefix() != null && key.equals(listDTO.getPrefix())) {
                    continue;
                }

                // ✅ 移除了错误的客户端过滤逻辑
                // S3的delimiter机制已经保证了contents只包含当前层级的文件
                // 不需要再检查 relativePath.contains("/")

                Map<String, Object> fileInfo = convertToFileInfo(obj);
                fileInfo.put("isFolder", false);
                allFiles.add(fileInfo);
            }

            // 2. 使用S3原生分页返回当前页（不做内存二次分页）
            List<Map<String, Object>> pageFolders = allFolders;
            List<Map<String, Object>> pageFiles = allFiles;

            Map<String, Object> pagination = new HashMap<>();
            int pageSize = listDTO.getPageSize() != null ? listDTO.getPageSize() : (listDTO.getMaxKeys() != null ? listDTO.getMaxKeys() : 100);
            pagination.put("pageSize", pageSize);
            pagination.put("hasMore", response.isTruncated());
            pagination.put("nextContinuationToken", response.nextMarker());  // V1 API使用nextMarker
            pagination.put("currentCount", pageFolders.size() + pageFiles.size());
            pagination.put("folderCount", pageFolders.size());
            pagination.put("fileCount", pageFiles.size());

            result.put("folders", pageFolders);
            result.put("files", pageFiles);
            result.put("pagination", pagination);

            log.info("文件列表获取成功 - 文件夹: {}, 文件: {}", pageFolders.size(), pageFiles.size());

            return result;

        } catch (Exception e) {
            log.error("获取文件列表失败 - backend: {}, bucketName: {}, prefix: {}",
                    listDTO.getBackendName(), listDTO.getBucketName(), listDTO.getPrefix(), e);
            throw new RuntimeException("获取文件列表失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteFile(String backendName, String bucketName, String objectKey) {
        StorageConfigProperties.Backend backend = getBackend(backendName);

        try {
            S3Client s3Client = s3ClientUtil.createS3Client(backend);
            String actualBucketName = bucketName != null ? bucketName : backend.getDefaultBucket();

            // 如果是文件夹（以/结尾），需要递归删除所有子文件
            if (objectKey.endsWith("/")) {
                deleteFolderRecursively(s3Client, actualBucketName, objectKey);
            } else {
                // 普通文件直接删除
                DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                        .bucket(actualBucketName)
                        .key(objectKey)
                        .build();

                s3Client.deleteObject(deleteObjectRequest);
            }

        } catch (Exception e) {
            log.error("文件删除失败 - backend: {}, bucket: {}, key: {}", backendName, bucketName, objectKey, e);
            throw new RuntimeException("文件删除失败: " + e.getMessage());
        }
    }

    /**
     * 递归删除文件夹及其所有内容
     */
    private void deleteFolderRecursively(S3Client s3Client, String bucketName, String prefix) {
        try {
            log.info("开始递归删除文件夹: bucket={}, prefix={}", bucketName, prefix);

            // 列出所有子对象（不使用delimiter，获取所有递归内容）
            software.amazon.awssdk.services.s3.model.ListObjectsV2Request listRequest =
                software.amazon.awssdk.services.s3.model.ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .build();

            software.amazon.awssdk.services.s3.model.ListObjectsV2Response listResponse;
            int totalDeleted = 0;

            do {
                listResponse = s3Client.listObjectsV2(listRequest);
                log.info("列出对象: 找到 {} 个对象", listResponse.contents().size());

                if (!listResponse.contents().isEmpty()) {
                    // 逐个删除，避免批量删除的URI问题
                    for (software.amazon.awssdk.services.s3.model.S3Object s3Object : listResponse.contents()) {
                        try {
                            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                                    .bucket(bucketName)
                                    .key(s3Object.key())
                                    .build();

                            s3Client.deleteObject(deleteRequest);
                            totalDeleted++;

                            if (totalDeleted % 10 == 0) {
                                log.info("已删除 {} 个对象", totalDeleted);
                            }
                        } catch (Exception e) {
                            log.error("删除对象失败: {}", s3Object.key(), e);
                            // 继续删除其他文件
                        }
                    }
                }

                // 如果还有更多对象，继续删除
                if (listResponse.isTruncated()) {
                    listRequest = listRequest.toBuilder()
                        .continuationToken(listResponse.nextContinuationToken())
                        .build();
                }
            } while (listResponse.isTruncated());

            log.info("文件夹删除完成，共删除 {} 个对象: {}", totalDeleted, prefix);

            // 如果一个对象都没删除，说明文件夹可能是空的或不存在
            if (totalDeleted == 0) {
                log.warn("文件夹为空或不存在: {}", prefix);
            }

        } catch (Exception e) {
            log.error("递归删除文件夹失败 - bucket: {}, prefix: {}", bucketName, prefix, e);
            throw new RuntimeException("递归删除文件夹失败: " + e.getMessage());
        }
    }

    @Override
    public void previewFile(String backendName, String bucketName, String objectKey, HttpServletResponse response) {
        StorageConfigProperties.Backend backend = getBackend(backendName);

        try {
            S3Client s3Client = s3ClientUtil.createS3Client(backend);
            String actualBucketName = bucketName != null ? bucketName : backend.getDefaultBucket();

            // 获取内容类型
            String contentType = "application/octet-stream";
            try {
                HeadObjectRequest headReq = HeadObjectRequest.builder()
                        .bucket(actualBucketName)
                        .key(objectKey)
                        .build();
                HeadObjectResponse headResp = s3Client.headObject(headReq);
                if (headResp.contentType() != null && !headResp.contentType().isEmpty()) {
                    contentType = headResp.contentType();
                }
            } catch (S3Exception ignore) {
                // 忽略HEAD失败，按默认类型预览
            }

            String fileName = objectKey.substring(objectKey.lastIndexOf('/') + 1);

            response.setContentType(contentType);
            response.setHeader("Content-Disposition", "inline; filename=" +
                    URLEncoder.encode(fileName, StandardCharsets.UTF_8));

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(actualBucketName)
                    .key(objectKey)
                    .build();

            // 检查是否需要压缩图片
            if (configProperties.getPreview().isEnableImageCompression() && isImage(contentType)) {
                compressAndOutputImage(s3Client, getObjectRequest, response);
            } else {
                s3Client.getObject(getObjectRequest, ResponseTransformer.toOutputStream(response.getOutputStream()));
            }
        } catch (Exception e) {
            log.error("文件预览失败 - backend: {}, bucket: {}, key: {}", backendName, bucketName, objectKey, e);
            throw new RuntimeException("文件预览失败: " + e.getMessage());
        }
    }

    /**
     * 判断是否为图片类型
     */
    private boolean isImage(String contentType) {
        if (contentType == null) {
            return false;
        }
        String lowerType = contentType.toLowerCase();
        return lowerType.startsWith("image/") &&
               !lowerType.equals("image/svg+xml") && // SVG不压缩
               !lowerType.equals("image/gif");       // GIF不压缩（保留动画）
    }

    /**
     * 压缩图片并输出到响应流
     */
    private void compressAndOutputImage(S3Client s3Client, GetObjectRequest getObjectRequest, HttpServletResponse response) {
        try {
            // 从S3下载图片到内存
            byte[] imageBytes = s3Client.getObject(getObjectRequest, ResponseTransformer.toBytes()).asByteArray();

            // 使用Thumbnailator压缩图片
            java.io.ByteArrayInputStream inputStream = new java.io.ByteArrayInputStream(imageBytes);
            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();

            net.coobird.thumbnailator.Thumbnails.of(inputStream)
                    .size(configProperties.getPreview().getImageMaxWidth(),
                          configProperties.getPreview().getImageMaxHeight())
                    .outputQuality(configProperties.getPreview().getImageQuality())
                    .toOutputStream(outputStream);

            // 输出压缩后的图片
            byte[] compressedBytes = outputStream.toByteArray();
            response.getOutputStream().write(compressedBytes);

            log.debug("图片压缩成功 - 原始大小: {} bytes, 压缩后: {} bytes",
                     imageBytes.length, compressedBytes.length);

        } catch (Exception e) {
            log.error("图片压缩失败，返回原图", e);
            // 压缩失败时，返回原图
            try {
                s3Client.getObject(getObjectRequest, ResponseTransformer.toOutputStream(response.getOutputStream()));
            } catch (IOException ex) {
                log.error("返回原图失败", ex);
                throw new RuntimeException(ex);
            }
        }
    }

    @Override
    public String getPresignedUrl(String backendName, String bucketName, String objectKey, int expirationSeconds) {
        StorageConfigProperties.Backend backend = getBackend(backendName);

        try(  S3Presigner presigner = s3ClientUtil.createS3Presigner(backend)) {
            String actualBucketName = bucketName != null ? bucketName : backend.getDefaultBucket();

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(actualBucketName)
                    .key(objectKey)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(expirationSeconds))
                    .getObjectRequest(getObjectRequest)
                    .build();

            return presigner.presignGetObject(presignRequest).url().toString();

        } catch (Exception e) {
            log.error("获取预签名URL失败 - backend: {}, bucket: {}, key: {}", backendName, bucketName, objectKey, e);
            throw new RuntimeException("获取预签名URL失败: " + e.getMessage());
        }
    }

    @Override
    public void createFolder(String backendName, String bucketName, String folderPath) {
        StorageConfigProperties.Backend backend = getBackend(backendName);

        try {
            S3Client s3Client = s3ClientUtil.createS3Client(backend);
            String actualBucketName = bucketName != null ? bucketName : backend.getDefaultBucket();

            // 确保文件夹路径以/结尾
            if (!folderPath.endsWith("/")) {
                folderPath += "/";
            }

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(actualBucketName)
                    .key(folderPath)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.empty());

        } catch (Exception e) {
            log.error("创建文件夹失败 - backend: {}, bucket: {}, folderPath: {}", backendName, bucketName, folderPath, e);
            throw new RuntimeException("创建文件夹失败: " + e.getMessage());
        }
    }

    @Override
    public List<String> listBuckets(String backendName) {
        StorageConfigProperties.Backend backend = getBackend(backendName);

        try {
            S3Client s3Client = s3ClientUtil.createS3Client(backend);
            ListBucketsRequest request = ListBucketsRequest.builder().build();
            ListBucketsResponse response = s3Client.listBuckets(request);

            return response.buckets().stream()
                    .map(Bucket::name)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("获取存储桶列表失败 - backend: {}", backendName, e);
            throw new RuntimeException("获取存储桶列表失败: " + e.getMessage());
        }
    }

    @Override
    public void batchDeleteFiles(String backendName, String bucketName, List<String> objectKeys) {
        StorageConfigProperties.Backend backend = getBackend(backendName);

        try {
            S3Client s3Client = s3ClientUtil.createS3Client(backend);
            String actualBucketName = bucketName != null ? bucketName : backend.getDefaultBucket();

            // 构建删除请求
            List<ObjectIdentifier> objectIdentifiers = objectKeys.stream()
                    .map(key -> ObjectIdentifier.builder().key(key).build())
                    .collect(Collectors.toList());

            Delete delete = Delete.builder()
                    .objects(objectIdentifiers)
                    .build();

            DeleteObjectsRequest deleteObjectsRequest = DeleteObjectsRequest.builder()
                    .bucket(actualBucketName)
                    .delete(delete)
                    .build();

            DeleteObjectsResponse response = s3Client.deleteObjects(deleteObjectsRequest);

            log.info("批量删除文件成功 - backend: {}, bucket: {}, 删除数量: {}",
                    backendName, actualBucketName, response.deleted().size());

        } catch (Exception e) {
            log.error("批量删除文件失败 - backend: {}, bucket: {}", backendName, bucketName, e);
            throw new RuntimeException("批量删除文件失败: " + e.getMessage());
        }
    }

    private String generateObjectKey(String originalFilename) {
        String extension = FilenameUtils.getExtension(originalFilename);
        String baseName = FilenameUtils.getBaseName(originalFilename);
        String timestamp = String.valueOf(System.currentTimeMillis());
        return baseName + "_" + timestamp + "." + extension;
    }

    private Map<String, Object> convertToFileInfo(S3Object s3Object) {
        Map<String, Object> fileInfo = new HashMap<>();
        fileInfo.put("key", s3Object.key());
        fileInfo.put("size", s3Object.size());
        fileInfo.put("lastModified", s3Object.lastModified());
        fileInfo.put("storageClass", s3Object.storageClassAsString());
        return fileInfo;
    }

    @Override
    public void renameFile(String backendName, String bucketName, String oldKey, String newKey) {
        StorageConfigProperties.Backend backend = getBackend(backendName);
        String actualBucket = (bucketName != null && !bucketName.isEmpty()) ? bucketName : backend.getDefaultBucket();

        if (actualBucket == null || actualBucket.isEmpty()) {
            throw new RuntimeException("存储桶名称不能为空");
        }

        log.info("开始重命名文件: bucket={}, oldKey={}, newKey={}", actualBucket, oldKey, newKey);

        try (S3Client s3Client = s3ClientUtil.createS3Client(backend)) {
            // 检查源文件是否存在
            try {
                s3Client.headObject(HeadObjectRequest.builder()
                        .bucket(actualBucket)
                        .key(oldKey)
                        .build());
            } catch (NoSuchKeyException e) {
                throw new RuntimeException("源文件不存在: " + oldKey);
            }

            // 检查目标文件是否已存在
            try {
                s3Client.headObject(HeadObjectRequest.builder()
                        .bucket(actualBucket)
                        .key(newKey)
                        .build());
                throw new RuntimeException("目标文件已存在: " + newKey);
            } catch (NoSuchKeyException e) {
                // 目标不存在，可以继续
            }

            // 如果是文件夹（以 / 结尾），需要递归重命名所有子对象
            if (oldKey.endsWith("/")) {
                renameFolder(s3Client, actualBucket, oldKey, newKey);
            } else {
                // 普通文件：复制 + 删除
                CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                        .sourceBucket(actualBucket)
                        .sourceKey(oldKey)
                        .destinationBucket(actualBucket)
                        .destinationKey(newKey)
                        .build();

                s3Client.copyObject(copyRequest);
                log.info("文件复制成功: {} -> {}", oldKey, newKey);

                // 删除源文件
                DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                        .bucket(actualBucket)
                        .key(oldKey)
                        .build();

                s3Client.deleteObject(deleteRequest);
                log.info("源文件删除成功: {}", oldKey);
            }

            log.info("文件重命名完成: {} -> {}", oldKey, newKey);
        } catch (Exception e) {
            log.error("重命名文件失败: oldKey={}, newKey={}", oldKey, newKey, e);
            throw new RuntimeException("重命名文件失败: " + e.getMessage(), e);
        }
    }

    private void renameFolder(S3Client s3Client, String bucket, String oldPrefix, String newPrefix) {
        log.info("开始重命名文件夹: oldPrefix={}, newPrefix={}", oldPrefix, newPrefix);

        // 列出所有以 oldPrefix 开头的对象
        ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(oldPrefix)
                .build();

        ListObjectsV2Response listResponse;
        int totalCount = 0;

        do {
            listResponse = s3Client.listObjectsV2(listRequest);
            List<S3Object> objects = listResponse.contents();

            for (S3Object s3Object : objects) {
                String oldKey = s3Object.key();
                String newKey = oldKey.replaceFirst("^" + oldPrefix, newPrefix);

                // 复制对象
                CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                        .sourceBucket(bucket)
                        .sourceKey(oldKey)
                        .destinationBucket(bucket)
                        .destinationKey(newKey)
                        .build();

                s3Client.copyObject(copyRequest);

                // 删除源对象
                DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(oldKey)
                        .build();

                s3Client.deleteObject(deleteRequest);

                totalCount++;
                log.debug("文件夹内对象重命名: {} -> {}", oldKey, newKey);
            }

            // 如果有更多对象，继续列表
            if (listResponse.isTruncated()) {
                listRequest = ListObjectsV2Request.builder()
                        .bucket(bucket)
                        .prefix(oldPrefix)
                        .continuationToken(listResponse.nextContinuationToken())
                        .build();
            }
        } while (listResponse.isTruncated());

        log.info("文件夹重命名完成，共处理 {} 个对象", totalCount);
    }

    @Override
    public long calculateFolderSize(String backendName, String bucketName, String folderPath) {
        StorageConfigProperties.Backend backend = getBackend(backendName);
        String actualBucket = (bucketName != null && !bucketName.isEmpty()) ? bucketName : backend.getDefaultBucket();

        if (actualBucket == null || actualBucket.isEmpty()) {
            throw new RuntimeException("存储桶名称不能为空");
        }

        log.info("开始计算文件夹大小: bucket={}, folderPath={}", actualBucket, folderPath);

        try (S3Client s3Client = s3ClientUtil.createS3Client(backend)) {
            long totalSize = 0;
            int fileCount = 0;

            // 列出文件夹下所有文件（递归）
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(actualBucket)
                    .prefix(folderPath)
                    .build();

            ListObjectsV2Response listResponse;

            do {
                listResponse = s3Client.listObjectsV2(listRequest);

                for (S3Object s3Object : listResponse.contents()) {
                    // 跳过文件夹标记本身
                    if (!s3Object.key().endsWith("/")) {
                        totalSize += s3Object.size();
                        fileCount++;
                    }
                }

                // 如果有更多数据，继续获取
                if (listResponse.isTruncated()) {
                    listRequest = ListObjectsV2Request.builder()
                            .bucket(actualBucket)
                            .prefix(folderPath)
                            .continuationToken(listResponse.nextContinuationToken())
                            .build();
                }

            } while (listResponse.isTruncated());

            log.info("文件夹大小计算完成: folderPath={}, totalSize={}, fileCount={}", folderPath, totalSize, fileCount);

            return totalSize;

        } catch (Exception e) {
            log.error("计算文件夹大小失败: bucket={}, folderPath={}", actualBucket, folderPath, e);
            throw new RuntimeException("计算文件夹大小失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void copyFile(String backendName, String sourceBucket, String sourceKey, String targetBucket, String targetKey) {
        log.info("开始复制文件: backend={}, source={}:{}, target={}:{}",
                backendName, sourceBucket, sourceKey, targetBucket, targetKey);

        StorageConfigProperties.Backend backend = getBackend(backendName);
        if (backend == null || !backend.getEnabled()) {
            throw new RuntimeException("存储后端不可用: " + backendName);
        }

        try (S3Client s3Client = s3ClientUtil.createS3Client(backend)) {
            // 使用 S3 CopyObject API
            CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                    .sourceBucket(sourceBucket)
                    .sourceKey(sourceKey)
                    .destinationBucket(targetBucket)
                    .destinationKey(targetKey)
                    .build();

            s3Client.copyObject(copyRequest);

            log.info("文件复制成功: source={}:{}, target={}:{}",
                    sourceBucket, sourceKey, targetBucket, targetKey);

        } catch (Exception e) {
            log.error("文件复制失败: source={}:{}, target={}:{}",
                    sourceBucket, sourceKey, targetBucket, targetKey, e);
            throw new RuntimeException("文件复制失败: " + e.getMessage(), e);
        }
    }

    @Override
    public int copyFolder(String backendName, String sourceBucket, String sourceFolderPath,
                         String targetBucket, String targetFolderPath) {
        log.info("开始复制文件夹: backend={}, source={}:{}, target={}:{}",
                backendName, sourceBucket, sourceFolderPath, targetBucket, targetFolderPath);

        // 确保路径以 / 结尾
        if (!sourceFolderPath.endsWith("/")) {
            sourceFolderPath += "/";
        }
        if (!targetFolderPath.endsWith("/")) {
            targetFolderPath += "/";
        }

        StorageConfigProperties.Backend backend = getBackend(backendName);
        if (backend == null || !backend.getEnabled()) {
            throw new RuntimeException("存储后端不可用: " + backendName);
        }

        int copiedCount = 0;

        try (S3Client s3Client = s3ClientUtil.createS3Client(backend)) {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(sourceBucket)
                    .prefix(sourceFolderPath)
                    .build();

            ListObjectsV2Response listResponse;

            do {
                listResponse = s3Client.listObjectsV2(listRequest);

                for (S3Object s3Object : listResponse.contents()) {
                    String sourceKey = s3Object.key();

                    // 跳过文件夹标记对象
                    if (sourceKey.endsWith("/")) {
                        continue;
                    }

                    // 计算目标路径
                    String relativePath = sourceKey.substring(sourceFolderPath.length());
                    String targetKey = targetFolderPath + relativePath;

                    // 复制单个文件
                    try {
                        CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                                .sourceBucket(sourceBucket)
                                .sourceKey(sourceKey)
                                .destinationBucket(targetBucket)
                                .destinationKey(targetKey)
                                .build();

                        s3Client.copyObject(copyRequest);
                        copiedCount++;

                        log.debug("文件复制成功: {} -> {}", sourceKey, targetKey);

                    } catch (Exception e) {
                        log.error("文件复制失败: {} -> {}, 错误: {}", sourceKey, targetKey, e.getMessage());
                        // 继续复制其他文件
                    }
                }

                // 继续下一页
                if (listResponse.isTruncated()) {
                    listRequest = listRequest.toBuilder()
                            .continuationToken(listResponse.nextContinuationToken())
                            .build();
                }

            } while (listResponse.isTruncated());

            log.info("文件夹复制完成: source={}:{}, target={}:{}, 共复制 {} 个文件",
                    sourceBucket, sourceFolderPath, targetBucket, targetFolderPath, copiedCount);

            return copiedCount;

        } catch (Exception e) {
            log.error("文件夹复制失败: source={}:{}, target={}:{}",
                    sourceBucket, sourceFolderPath, targetBucket, targetFolderPath, e);
            throw new RuntimeException("文件夹复制失败: " + e.getMessage(), e);
        }
    }
}
