package com.example.backend.controller;

import com.example.backend.dto.CommentRequest;
import com.example.backend.dto.CommentResponse;
import com.example.backend.dto.PostRequest;
import com.example.backend.dto.PostResponse;
import com.example.backend.service.AuthService;
import com.example.backend.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Slf4j
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final AuthService authService;

    @GetMapping
    public ResponseEntity<List<PostResponse>> getAllPosts(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sortBy", required = false, defaultValue = "latest") String sortBy) {
        List<PostResponse> posts = postService.getAllPosts(keyword, sortBy);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/popular")
    public ResponseEntity<List<PostResponse>> getPopularPosts(
            @RequestParam(value = "keyword", required = false) String keyword) {
        List<PostResponse> posts = postService.getAllPosts(keyword, "views");
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getPostById(@PathVariable("id") Long id) {
        PostResponse post = postService.getPostById(id);
        return ResponseEntity.ok(post);
    }

    @PostMapping
    public ResponseEntity<PostResponse> createPost(
            @Valid @RequestBody PostRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String currentUsername = authService.extractUsernameFromHeader(authHeader);
        PostResponse created = postService.createPost(request, currentUsername);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable("id") Long id,
            @Valid @RequestBody PostRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String currentUsername = authService.extractUsernameFromHeader(authHeader);
        PostResponse updated = postService.updatePost(id, request, currentUsername);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String currentUsername = authService.extractUsernameFromHeader(authHeader);
        postService.deletePost(id, currentUsername);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable("id") Long id) {
        return ResponseEntity.ok(postService.getComments(id));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable("id") Long id,
            @Valid @RequestBody CommentRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String currentUsername = authService.extractUsernameFromHeader(authHeader);
        CommentResponse created = postService.createComment(id, request, currentUsername);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable("id") Long id,
            @PathVariable("commentId") Long commentId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String currentUsername = authService.extractUsernameFromHeader(authHeader);
        postService.deleteComment(id, commentId, currentUsername);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException ex) {
        log.warn("Entity not found: {}", ex.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put("error", "NOT_FOUND");
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> handleUnauthorized(SecurityException ex) {
        log.warn("Unauthorized request: {}", ex.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put("error", "UNAUTHORIZED");
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleForbidden(IllegalStateException ex) {
        log.warn("Forbidden request: {}", ex.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put("error", "FORBIDDEN");
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err ->
            fieldErrors.put(err.getField(), err.getDefaultMessage())
        );
        log.warn("Validation failed: {}", fieldErrors);

        Map<String, Object> response = new HashMap<>();
        response.put("error", "VALIDATION_FAILED");
        response.put("message", String.join(", ", fieldErrors.values()));
        response.put("fields", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
