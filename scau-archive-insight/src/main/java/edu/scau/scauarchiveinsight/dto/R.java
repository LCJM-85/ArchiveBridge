package edu.scau.scauarchiveinsight.dto;

import lombok.Data;

@Data
public class R<T> {
    private int code;
    private T data;
    private String msg;

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.code = 200;
        r.data = data;
        r.msg = "success";
        return r;
    }

    public static <T> R<T> ok(T data, String msg) {
        R<T> r = ok(data);
        r.msg = msg;
        return r;
    }

    public static <T> R<T> error(String msg) {
        R<T> r = new R<>();
        r.code = 500;
        r.msg = msg;
        return r;
    }

    public static <T> R<T> error(int code, String msg) {
        R<T> r = new R<>();
        r.code = code;
        r.msg = msg;
        return r;
    }
}
