package com.example.demo.exception;

public class NewsNotFoundException extends RuntimeException {
    public NewsNotFoundException(Long id) {
        super("News not found with id: " + id);
    }
}