package com.example.demo.exception;

public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException() {
        super("Too many login attempts. Try again later.");
    }
}