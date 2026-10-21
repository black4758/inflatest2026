package com.example.backend.dto;

import com.example.backend.domain.Comment;

import java.time.LocalDateTime;

public record CommentResponse(
    Long id,
    Long postId,
    String content,
    String author,
    LocalDateTime createdAt
) {
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
            comment.getId(),
            comment.getPost().getId(),
            comment.getContent(),
            comment.getAuthor(),
            comment.getCreatedAt()
        );
    }
}
