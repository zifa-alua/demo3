package com.example.demo.service;

import com.example.demo.dto.NewsRequestDto;
import com.example.demo.dto.NewsResponseDto;
import com.example.demo.entity.News;
import com.example.demo.exception.NewsNotFoundException;
import com.example.demo.repository.NewsRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NewsService {
    private final NewsRepository newsRepository;
    public NewsService(NewsRepository newsRepository) {
        this.newsRepository = newsRepository;
    }
    public NewsResponseDto create(NewsRequestDto dto) {
        News news = new News();
        news.setTitle(dto.getTitle());
        news.setContent(dto.getContent());
        news.setPublishedDate(dto.getPublishedDate());
        news.setAuthor(dto.getAuthor());
        News saved = newsRepository.save(news);
        return toResponseDto(saved);
    }
    public List<NewsResponseDto> getAll() {
        return newsRepository.findAll()
                .stream()
                .map(this::toResponseDto)
                .toList();
    }
    public NewsResponseDto getById(Long id) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new NewsNotFoundException(id));
        return toResponseDto(news);
    }
    public NewsResponseDto update(Long id, NewsRequestDto dto) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new NewsNotFoundException(id));
        news.setTitle(dto.getTitle());
        news.setContent(dto.getContent());
        news.setPublishedDate(dto.getPublishedDate());
        news.setAuthor(dto.getAuthor());
        News updated = newsRepository.save(news);
        return toResponseDto(updated);
    }
    public void delete(Long id) {
        if (!newsRepository.existsById(id)) {
            throw new NewsNotFoundException(id);
        }
        newsRepository.deleteById(id);
    }
    private NewsResponseDto toResponseDto(News news) {
        return new NewsResponseDto(
                news.getId(),
                news.getTitle(),
                news.getContent(),
                news.getPublishedDate(),
                news.getAuthor(),
                news.getCreatedAt()
        );
    }
}