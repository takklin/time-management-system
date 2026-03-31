package com.timemanager.common.exception;

import com.timemanager.common.result.Result;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public Result<Void> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        // 可以打印日志、统计等
        ex.printStackTrace();
        return Result.error(413, "上传文件太大，最大支持 10MB，请压缩后重试");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleGeneralException(Exception ex) {
        ex.printStackTrace();
        return Result.error(500, "服务器内部错误，请稍后重试");
    }
}
