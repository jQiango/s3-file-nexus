package com.all.in.one.agent.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * 文件项 DTO（支持文件和文件夹）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 显示名称
     */
    private String name;

    /**
     * 完整路径/Key
     */
    private String key;

    /**
     * 类型: "folder" 或 "file"
     */
    private String type;

    /**
     * 文件大小（字节）
     */
    private Long size;

    /**
     * 最后修改时间
     */
    private Instant lastModified;

    /**
     * 内容类型
     */
    private String contentType;

    /**
     * 文件夹统计信息（仅文件夹有）
     */
    private FolderStats folderStats;

    /**
     * 是否为文件夹
     */
    public boolean isFolder() {
        return "folder".equals(type);
    }

    /**
     * 从前缀创建文件夹项
     */
    public static FileItem folder(String prefix, FolderStats stats) {
        return FileItem.builder()
                .name(extractFolderName(prefix))
                .key(prefix)
                .type("folder")
                .folderStats(stats)
                .build();
    }

    /**
     * 提取文件夹名称（去掉前缀和尾部斜杠）
     */
    private static String extractFolderName(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return "";
        }
        // 去掉尾部斜杠
        String name = prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
        // 取最后一个斜杠后的部分
        int lastSlash = name.lastIndexOf('/');
        return lastSlash >= 0 ? name.substring(lastSlash + 1) : name;
    }
}
