package com.example.backend.service;

import com.example.backend.domain.Comment;
import com.example.backend.domain.Post;
import com.example.backend.dto.CommentRequest;
import com.example.backend.dto.CommentResponse;
import com.example.backend.dto.PostRequest;
import com.example.backend.dto.PostResponse;
import com.example.backend.repository.CommentRepository;
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
    private final CommentRepository commentRepository;

    public List<PostResponse> getAllPosts(String keyword, String sortBy) {
        boolean isViewsSort = "views".equalsIgnoreCase(sortBy) || "viewCount".equalsIgnoreCase(sortBy);
        List<Post> posts;
        if (keyword != null && !keyword.trim().isEmpty()) {
            log.info("Searching posts with keyword: {}, sortBy: {}", keyword, sortBy);
            if (isViewsSort) {
                posts = postRepository.searchPostsOrderByViewCountDesc(keyword.trim());
            } else {
                posts = postRepository.searchPosts(keyword.trim());
            }
        } else {
            if (isViewsSort) {
                posts = postRepository.findAllByOrderByViewCountDescCreatedAtDesc();
            } else {
                posts = postRepository.findAllByOrderByCreatedAtDesc();
            }
        }
        return posts.stream()
            .map(post -> PostResponse.from(post, commentRepository.countByPostId(post.getId())))
            .toList();
    }

    public List<PostResponse> getAllPosts(String keyword) {
        return getAllPosts(keyword, "latest");
    }

    @Transactional
    public PostResponse getPostById(Long id) {
        log.info("Fetching post detail for ID: {}", id);
        Post post = postRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없습니다. ID: " + id));
        postRepository.incrementViewCount(id);
        post.incrementViewCount();

        List<CommentResponse> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(id)
            .stream()
            .map(CommentResponse::from)
            .toList();

        return PostResponse.from(post, comments.size(), comments);
    }

    @Transactional
    public PostResponse createPost(PostRequest request, String currentUsername) {
        if (currentUsername == null || currentUsername.isBlank()) {
            throw new SecurityException("게시글을 작성하려면 로그인이 필요합니다.");
        }
        log.info("Creating new post by author: {}", currentUsername);
        Post post = Post.builder()
            .title(request.title().trim())
            .content(request.content().trim())
            .author(currentUsername)
            .build();
        Post saved = postRepository.save(post);
        return PostResponse.from(saved, 0L);
    }

    @Transactional
    public PostResponse updatePost(Long id, PostRequest request, String currentUsername) {
        if (currentUsername == null || currentUsername.isBlank()) {
            throw new SecurityException("게시글을 수정하려면 로그인이 필요합니다.");
        }
        Post post = postRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없습니다. ID: " + id));

        if (!post.getAuthor().equals(currentUsername)) {
            throw new IllegalStateException("본인이 작성한 게시글만 수정할 수 있습니다.");
        }

        log.info("Updating post ID: {} by author: {}", id, currentUsername);
        post.update(
            request.title().trim(),
            request.content().trim(),
            currentUsername
        );

        List<CommentResponse> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(id)
            .stream()
            .map(CommentResponse::from)
            .toList();

        return PostResponse.from(post, comments.size(), comments);
    }

    @Transactional
    public void deletePost(Long id, String currentUsername) {
        if (currentUsername == null || currentUsername.isBlank()) {
            throw new SecurityException("게시글을 삭제하려면 로그인이 필요합니다.");
        }
        Post post = postRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없습니다. ID: " + id));

        if (!post.getAuthor().equals(currentUsername)) {
            throw new IllegalStateException("본인이 작성한 게시글만 삭제할 수 있습니다.");
        }

        log.info("Deleting post ID: {} by author: {}", id, currentUsername);
        commentRepository.deleteByPostId(id);
        postRepository.delete(post);
    }

    public List<CommentResponse> getComments(Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new NoSuchElementException("게시글을 찾을 수 없습니다. ID: " + postId);
        }
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId)
            .stream()
            .map(CommentResponse::from)
            .toList();
    }

    @Transactional
    public CommentResponse createComment(Long postId, CommentRequest request, String currentUsername) {
        if (currentUsername == null || currentUsername.isBlank()) {
            throw new SecurityException("댓글을 작성하려면 로그인이 필요합니다.");
        }
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없습니다. ID: " + postId));

        log.info("Creating comment on post ID: {} by author: {}", postId, currentUsername);
        Comment comment = Comment.builder()
            .post(post)
            .content(request.content().trim())
            .author(currentUsername)
            .build();
        Comment saved = commentRepository.save(comment);
        return CommentResponse.from(saved);
    }

    @Transactional
    public void deleteComment(Long postId, Long commentId, String currentUsername) {
        if (currentUsername == null || currentUsername.isBlank()) {
            throw new SecurityException("댓글을 삭제하려면 로그인이 필요합니다.");
        }
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new NoSuchElementException("댓글을 찾을 수 없습니다. ID: " + commentId));

        if (!comment.getPost().getId().equals(postId)) {
            throw new IllegalArgumentException("해당 게시글의 댓글이 아닙니다.");
        }

        if (!comment.getAuthor().equals(currentUsername)) {
            throw new IllegalStateException("본인이 작성한 댓글만 삭제할 수 있습니다.");
        }

        log.info("Deleting comment ID: {} on post ID: {} by author: {}", commentId, postId, currentUsername);
        commentRepository.delete(comment);
    }
}
