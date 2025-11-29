package com.all.in.one.agent.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 文件列表响应（优化版）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileListResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 混合内容（文件夹 + 文件）
     */
    private List<FileItem> items;

    /**
     * 下一页的 continuation token
     */
    private String nextContinuationToken;

    /**
     * 是否还有更多数据
     */
    private Boolean isTruncated;

    /**
     * 当前页数量
     */
    private Integer totalCount;

    /**
     * 是否来自缓存
     */
    private Boolean fromCache;
}
