package com.example.demo.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class NewsResponseDto {
    private Long id;
    private String title;
    private String content;
    private LocalDate publishedDate;
    private String author;
    private LocalDateTime createdAt;
    public NewsResponseDto() {}
    public NewsResponseDto(Long id, String title, String content, LocalDate publishedDate, String author, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.publishedDate = publishedDate;
        this.author = author;
        this.createdAt = createdAt;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDate getPublishedDate() { return publishedDate; }
    public void setPublishedDate(LocalDate publishedDate) { this.publishedDate = publishedDate; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
