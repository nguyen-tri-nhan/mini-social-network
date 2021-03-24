package com.nhan.minisocial.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class MiniSocialException extends RuntimeException{

    public MiniSocialException(String message) {
        super(message);
    }
    public MiniSocialException(String message, Throwable cause) {
        super(message, cause);
    }
}
