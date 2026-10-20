package com.example.backend.service;

import com.example.backend.domain.Post;
import com.example.backend.dto.PostRequest;
import com.example.backend.dto.PostResponse;
import com.example.backend.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;

    public List<PostResponse> getAllPosts(String keyword) {
        List<Post> posts;
        if (keyword != null && !keyword.trim().isEmpty()) {
            log.info("Searching posts with keyword: {}", keyword);
            posts = postRepository.searchPosts(keyword.trim());
        } else {
            posts = postRepository.findAllByOrderByCreatedAtDesc();
        }
        return posts.stream().map(PostResponse::from).toList();
    }

    @Transactional
    public PostResponse getPostById(Long id) {
        Post post = postRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없습니다. ID: " + id));
        postRepository.incrementViewCount(id);
        post.incrementViewCount();
        return PostResponse.from(post);
    }

    @Transactional
    public PostResponse createPost(PostRequest request) {
        log.info("Creating new post by author: {}", request.author());
        Post post = Post.builder()
            .title(request.title().trim())
            .content(request.content().trim())
            .author(request.author().trim())
            .build();
        Post saved = postRepository.save(post);
        return PostResponse.from(saved);
    }

    @Transactional
    public PostResponse updatePost(Long id, PostRequest request) {
        log.info("Updating post ID: {}", id);
        Post post = postRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없습니다. ID: " + id));
        post.update(
            request.title().trim(),
            request.content().trim(),
            request.author().trim()
        );
        return PostResponse.from(post);
    }

    @Transactional
    public void deletePost(Long id) {
        log.info("Deleting post ID: {}", id);
        if (!postRepository.existsById(id)) {
            throw new NoSuchElementException("게시글을 찾을 수 없습니다. ID: " + id);
        }
        postRepository.deleteById(id);
    }
}
