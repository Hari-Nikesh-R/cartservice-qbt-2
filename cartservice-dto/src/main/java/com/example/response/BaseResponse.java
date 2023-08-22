package com.example.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@Data
@AllArgsConstructor
@ToString
public class BaseResponse<T> {
    private T data;
    private Integer statusCode;
    private String errorDesc;
    private boolean success;
}
