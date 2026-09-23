package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class NewsRequestDto {
    @NotBlank(message = "Title must not be blank")
    @Size(max = 255, message = "Title must be at most 255 characters")
    private String title;
    @NotBlank(message = "Content must not be blank")
    private String content;
    @PastOrPresent(message = "Published date cannot be in the future")
    private LocalDate publishedDate;
    @NotBlank(message = "Author must not be blank")
    @Size(max = 100)
    private String author;
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDate getPublishedDate() { return publishedDate; }
    public void setPublishedDate(LocalDate publishedDate) { this.publishedDate = publishedDate; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
}