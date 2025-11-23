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
            throw new RuntimeException("存储后端不存在: " + backendName);
        }
        if (!Boolean.TRUE.equals(backend.getEnabled())) {
            throw new RuntimeException("存储后端未启用: " + backendName);
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

            var requestBuilder = ListObjectsV2Request.builder()
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
                requestBuilder = requestBuilder.continuationToken(listDTO.getContinuationToken());
                log.debug("设置继续标记: {}", listDTO.getContinuationToken());
            }

            ListObjectsV2Request request = requestBuilder.build();
            log.debug("S3请求参数: {}", request);

            ListObjectsV2Response response = s3Client.listObjectsV2(request);

            log.debug("S3响应 - 文件数量: {}, 文件夹数量: {}, 是否截断: {}",
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
            for (S3Object obj : response.contents()) {
                String key = obj.key();
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
            }
            // 如果没有commonPrefixes，尝试从文件列表中提取
            if (allFolders.isEmpty()) {
                Set<String> folderSet = new HashSet<>();
                for (S3Object obj : response.contents()) {
                    String key = obj.key();
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
                }
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
            }

            // 提取当前目录下的文件
            for (S3Object obj : response.contents()) {
                String key = obj.key();
                if (key.endsWith("/")) continue;
                if (listDTO.getPrefix() != null && key.equals(listDTO.getPrefix())) continue;
                if (listDTO.getPrefix() != null && !listDTO.getPrefix().isEmpty()) {
                    String relativePath = key.substring(listDTO.getPrefix().length());
                    if (relativePath.contains("/")) continue;
                }
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
            pagination.put("nextContinuationToken", response.nextContinuationToken());
            pagination.put("currentCount", pageFolders.size() + pageFiles.size());
            pagination.put("folderCount", pageFolders.size());
            pagination.put("fileCount", pageFiles.size());

            result.put("folders", pageFolders);
            result.put("files", pageFiles);
            result.put("pagination", pagination);
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

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(actualBucketName)
                    .key(objectKey)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);

        } catch (Exception e) {
            log.error("文件删除失败 - backend: {}, bucket: {}, key: {}", backendName, bucketName, objectKey, e);
            throw new RuntimeException("文件删除失败: " + e.getMessage());
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

            s3Client.getObject(getObjectRequest, ResponseTransformer.toOutputStream(response.getOutputStream()));
        } catch (Exception e) {
            log.error("文件预览失败 - backend: {}, bucket: {}, key: {}", backendName, bucketName, objectKey, e);
            throw new RuntimeException("文件预览失败: " + e.getMessage());
        }
    }

    @Override
    public String getPresignedUrl(String backendName, String bucketName, String objectKey, int expirationSeconds) {
        StorageConfigProperties.Backend backend = getBackend(backendName);

        try {
            S3Presigner presigner = s3ClientUtil.createS3Presigner(backend);
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
}
