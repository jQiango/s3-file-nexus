package com.all.in.one.agent.storage.common;

import lombok.Data;

/**
 * 统一响应结果
 */
@Data
public class Result<T> {

    /**
     * 状态码
     */
    private Integer code;

    /**
     * 消息
     */
    private String message;

    /**
     * 数据
     */
    private T data;

    /**
     * 是否成功
     */
    private Boolean success;

    public Result() {
    }

    public Result(Integer code, String message, T data, Boolean success) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.success = success;
    }

    /**
     * 成功返回
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null, true);
    }

    /**
     * 成功返回（带数据）
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data, true);
    }

    /**
     * 成功返回（带消息和数据）
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data, true);
    }

    /**
     * 失败返回
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null, false);
    }

    /**
     * 失败返回（带状态码）
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null, false);
    }
}
