package com.oceanviewresort.util;

public class ApiResponse<T> {
    private String status = "E";   // Default "E" for error, "S" for success
    private String text = "";
    private String code = "";
    private T result;

    public ApiResponse() {}

    public ApiResponse(String status, String text, String code, T result) {
        this.status = status;
        this.text = text;
        this.code = code;
        this.result = result;
    }

    // Getters and setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public T getResult() { return result; }
    public void setResult(T result) { this.result = result; }
}
