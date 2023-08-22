package com.example;

public class OutOfQuantityException extends Exception{
    public OutOfQuantityException(String message) {
        super(message);
    }
}
