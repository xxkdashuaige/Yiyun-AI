package com.yiyun.common;

import lombok.Data;

/**
 * 统一响应结构
 * <p>
 * 所有接口的返回值都使用此结构封装，保证前后端交互格式一致。
 *
 * @param <T> 业务数据的泛型类型
 */
@Data
public class Result<T> {

    /** 状态码：200 表示成功，其他表示失败 */
    private Integer code;

    /** 提示信息 */
    private String message;

    /** 业务数据 */
    private T data;

    /** 成功返回（带数据） */
    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.setCode(200);
        r.setMessage("成功");
        r.setData(data);
        return r;
    }

    /** 成功返回（无数据），内部调用带参的 success(null) */
    public static <T> Result<T> success() {
        return success(null);
    }

    /** 失败返回（自定义状态码和消息） */
    public static <T> Result<T> error(Integer code, String message) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }

    /** 失败返回（默认 500 状态码），内部调用 error(500, message) */
    public static <T> Result<T> error(String message) {
        return error(500, message);
    }
}