package com.all.in.one.agent.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 文件夹统计信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FolderStats implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文件数量
     */
    private Integer fileCount;

    /**
     * 总大小（字节）
     */
    private Long totalSize;

    /**
     * 是否正在计算中
     */
    private Boolean calculating;
}
