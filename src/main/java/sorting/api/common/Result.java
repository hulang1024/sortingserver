package sorting.api.common;

import lombok.Data;

/**
 * 表示一个API请求结果
 */
@Data
public class Result {
    private int code;
    private Object data;
    private String msg;

    private Result(int code, Object data) {
        this.code = code;
        this.data = data;
    }

    public Result message(String msg) {
        this.msg = msg;
        return this;
    }

    public static Result from(boolean ok) {
        return ok ? ok() : fail();
    }

    public static Result ok() {
        return new Result(0, null);
    }

    public static Result ok(Object data) {
        return new Result(0, data);
    }

    public static Result fail() {
        return new Result(1, null);
    }

    public static Result fail(int code) {
        assert code != 0;
        return new Result(code, null);
    }

    public static Result fail(int code, Object data) {
        assert code != 0;
        return new Result(code, data);
    }

    public static Result fail(Object data) {
        return new Result(1, data);
    }
}