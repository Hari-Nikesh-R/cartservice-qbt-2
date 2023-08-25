package com.example;

public class OutOfQuantityException extends RuntimeException{
    public OutOfQuantityException(String message) {
        super(message);
    }
}
