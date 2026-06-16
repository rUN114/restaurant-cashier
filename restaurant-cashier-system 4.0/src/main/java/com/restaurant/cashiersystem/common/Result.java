package com.restaurant.cashiersystem.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一API响应结果包装类
 * 格式：{ code: 200, msg: "success", data: T, success: true/false }
 */
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Result<T> {

    private int code;       // 状态码，200表示成功，其他表示错误
    private String msg;     // 提示信息
    private T data;         // 具体数据
    private Boolean success;// 成功标识

    private Result(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
        this.success = code == 200;
    }

    // 成功（无数据）
    public static <T> Result<T> success() {
        return new Result<>(200, "success", null);
    }

    // 成功（带数据）
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    // 成功（自定义消息）
    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(200, msg, data);
    }

    // 失败（默认消息）
    public static <T> Result<T> error() {
        return new Result<>(500, "系统错误", null);
    }

    // 失败（自定义消息）
    public static <T> Result<T> error(String msg) {
        return new Result<>(500, msg, null);
    }

    // 失败（自定义状态码和消息）
    public static <T> Result<T> error(int code, String msg) {
        return new Result<>(code, msg, null);
    }

    // 业务校验失败（如参数错误），状态码可自定义为400
    public static <T> Result<T> badRequest(String msg) {
        return new Result<>(400, msg, null);
    }

    // 未授权（token无效等）
    public static <T> Result<T> unauthorized(String msg) {
        return new Result<>(401, msg, null);
    }

    // 判断是否成功（供 Java 代码调用，序列化时使用 success 字段）
    @JsonIgnore
    public boolean isSuccess() {
        return this.code == 200;
    }
}