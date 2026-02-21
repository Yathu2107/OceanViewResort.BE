package com.oceanviewresort.exception;

public class UserInactiveException extends RuntimeException {

    public UserInactiveException(String message) {
        super(message);
    }

    public UserInactiveException(String message, Throwable cause) {
        super(message, cause);
    }
}
