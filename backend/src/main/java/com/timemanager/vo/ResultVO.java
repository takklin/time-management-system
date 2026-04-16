package com.timemanager.vo;

import lombok.Data;

@Data
public class ResultVO<T> {
    private int code;
    private String message;
    private T data;

    public static <T> ResultVO<T> success(T data) {
        ResultVO<T> vo = new ResultVO<>();
        vo.setCode(200);
        vo.setMessage("success");
        vo.setData(data);
        return vo;
    }

    public static <T> ResultVO<T> error(String message) {
        ResultVO<T> vo = new ResultVO<>();
        vo.setCode(500);
        vo.setMessage(message);
        return vo;
    }
}
