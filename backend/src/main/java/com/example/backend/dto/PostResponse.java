package com.example.backend.dto;

import com.example.backend.domain.Post;
import java.time.LocalDateTime;

public record PostResponse(
    Long id,
    String title,
    String content,
    String author,
    int viewCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static PostResponse from(Post post) {
        return new PostResponse(
            post.getId(),
            post.getTitle(),
            post.getContent(),
            post.getAuthor(),
            post.getViewCount(),
            post.getCreatedAt(),
            post.getUpdatedAt()
        );
    }
}
