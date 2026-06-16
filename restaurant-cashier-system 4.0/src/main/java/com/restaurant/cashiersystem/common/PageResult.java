package com.restaurant.cashiersystem.common;

import lombok.Getter;

/**
 * 分页响应包装类
 * 格式：{ code, msg, data (List), success, total, pages, pageNum, pageSize }
 */
@Getter
public class PageResult<T> {

    private final int code;
    private final String msg;
    private final T data;
    private final Boolean success;
    private final long total;
    private final long pages;
    private final int pageNum;
    private final int pageSize;

    private PageResult(int code, String msg, T data, long total, long pages, int pageNum, int pageSize) {
        this.code = code;
        this.msg = msg;
        this.data = data;
        this.success = code == 200;
        this.total = total;
        this.pages = pages;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

    /**
     * 分页成功响应
     */
    public static <T> PageResult<T> success(T data, long total, long pages, int pageNum, int pageSize) {
        return new PageResult<>(200, "success", data, total, pages, pageNum, pageSize);
    }
}
