package com.example.backend.dto;

import com.example.backend.domain.Post;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public record PostResponse(
    Long id,
    String title,
    String content,
    String author,
    int viewCount,
    long commentCount,
    List<CommentResponse> comments,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static PostResponse from(Post post) {
        return from(post, 0L, Collections.emptyList());
    }

    public static PostResponse from(Post post, long commentCount) {
        return from(post, commentCount, Collections.emptyList());
    }

    public static PostResponse from(Post post, long commentCount, List<CommentResponse> comments) {
        return new PostResponse(
            post.getId(),
            post.getTitle(),
            post.getContent(),
            post.getAuthor(),
            post.getViewCount(),
            commentCount,
            comments,
            post.getCreatedAt(),
            post.getUpdatedAt()
        );
    }
}
