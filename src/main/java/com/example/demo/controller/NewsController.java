package com.example.demo.controller;

import jakarta.validation.Valid;
import com.example.demo.dto.NewsRequestDto;
import com.example.demo.dto.NewsResponseDto;
import com.example.demo.service.NewsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @PostMapping
    public ResponseEntity<NewsResponseDto> create(@Valid @RequestBody NewsRequestDto dto) {
        NewsResponseDto created = newsService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<NewsResponseDto>> getAll() {
        return ResponseEntity.ok(newsService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<NewsResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(newsService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<NewsResponseDto> update(@PathVariable Long id, @Valid @RequestBody NewsRequestDto dto) {
        return ResponseEntity.ok(newsService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        newsService.delete(id);
        return ResponseEntity.noContent().build();
    }
}